package dev.healthcare.analytics.platform.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.healthcare.analytics.platform.analyticsschema.service.AppointmentFunnelReadService;
import dev.healthcare.analytics.platform.analyticsschema.view.AppointmentFunnelSnapshot;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * DashboardSseController exposes a server-sent events stream that pushes
 * near real-time analytics snapshots to web clients.
 */
@RestController
@RequestMapping("/api/analytics/stream")
public class DashboardSseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardSseController.class);

    private final AppointmentFunnelReadService funnelReadService;
    private final ObjectMapper objectMapper;

    private final List<ClientSubscription> subscriptions = new CopyOnWriteArrayList<>();

    public DashboardSseController(AppointmentFunnelReadService funnelReadService,
                                  ObjectMapper objectMapper,
                                  MeterRegistry meterRegistry) {
        this.funnelReadService = funnelReadService;
        this.objectMapper = objectMapper;

        Gauge.builder("platform.dashboard.sse.subscriptions", subscriptions, List::size)
                .description("Number of active SSE dashboard subscriptions")
                .register(meterRegistry);
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAnalytics(@RequestParam("organizationId") String organizationId,
                                      @RequestParam(value = "clinicId", required = false) String clinicId) {
        String effectiveClinicId = clinicId != null ? clinicId : "*";

        SseEmitter emitter = new SseEmitter(0L);
        ClientSubscription subscription = new ClientSubscription(emitter, organizationId, effectiveClinicId);
        subscriptions.add(subscription);

        emitter.onCompletion(() -> subscriptions.remove(subscription));
        emitter.onTimeout(() -> subscriptions.remove(subscription));
        emitter.onError(throwable -> subscriptions.remove(subscription));

        // Send an initial snapshot immediately.
        pushSnapshotToClient(subscription);

        return emitter;
    }

    @Scheduled(fixedDelayString = "${platform.dashboard.sse-broadcast-interval-ms:2000}")
    public void broadcastSnapshots() {
        for (ClientSubscription subscription : subscriptions) {
            pushSnapshotToClient(subscription);
        }
    }

    private void pushSnapshotToClient(ClientSubscription subscription) {
        try {
            if ("*".equals(subscription.clinicId())) {
                // For a wildcard clinic we currently skip broadcasting to keep the logic simple.
                return;
            }

            AppointmentFunnelSnapshot snapshot = funnelReadService.loadSnapshot(
                    subscription.organizationId(),
                    subscription.clinicId());

            String json = objectMapper.writeValueAsString(snapshot);
            subscription.emitter().send(SseEmitter.event()
                    .name("analytics-snapshot")
                    .id(Instant.now().toString())
                    .data(json));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to serialize analytics snapshot for org={} clinic={}",
                    subscription.organizationId(), subscription.clinicId(), e);
        } catch (IOException e) {
            LOGGER.debug("Removing closed SSE subscription for org={} clinic={}",
                    subscription.organizationId(), subscription.clinicId());
            subscriptions.remove(subscription);
        }
    }

    private record ClientSubscription(SseEmitter emitter, String organizationId, String clinicId) {
    }
}
