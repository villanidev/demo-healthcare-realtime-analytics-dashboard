package dev.healthcare.analytics.platform.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;

/**
 * DashboardSseController exposes a server-sent events stream that pushes
 * near real-time analytics snapshots to web clients.
 */
@RestController
@RequestMapping("/api/analytics/stream")
public class DashboardSseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardSseController.class);

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAnalytics(@RequestParam("organizationId") String organizationId,
                                      @RequestParam(value = "clinicId", required = false) String clinicId) {
        SseEmitter emitter = new SseEmitter();

        // For the POC skeleton, emit a single heartbeat payload.
        try {
            String placeholderJson = "{\"organizationId\":\"" + organizationId + "\"," +
                    "\"clinicId\":\"" + (clinicId == null ? "*" : clinicId) + "\"," +
                    "\"timestamp\":\"" + Instant.now() + "\"," +
                    "\"message\":\"analytics-stream-placeholder\"}";
            emitter.send(SseEmitter.event().name("analytics-snapshot").data(placeholderJson));
            emitter.complete();
        } catch (IOException ex) {
            LOGGER.warn("Failed to send initial SSE snapshot for org={} clinic={}", organizationId, clinicId, ex);
            emitter.completeWithError(ex);
        }

        return emitter;
    }
}
