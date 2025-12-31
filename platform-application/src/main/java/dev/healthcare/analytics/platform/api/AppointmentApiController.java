package dev.healthcare.analytics.platform.api;

import dev.healthcare.analytics.platform.appschema.core.AppointmentEntity;
import dev.healthcare.analytics.platform.domain.appointment.AppointmentApplicationService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentApiController {

    private final AppointmentApplicationService appointmentApplicationService;
    private final AppointmentApiMapper appointmentApiMapper;

    private final BlockingQueue<AppointmentApplicationService.ScheduleCommand> scheduleQueue;
    private final boolean scheduleQueueEnabled;
    private final long queueOfferTimeoutMillis;
        private final io.micrometer.core.instrument.Counter scheduleQueueFullCounter;
        public AppointmentApiController(AppointmentApplicationService appointmentApplicationService,
                        AppointmentApiMapper appointmentApiMapper,
                        BlockingQueue<AppointmentApplicationService.ScheduleCommand> scheduleQueue,
                        MeterRegistry meterRegistry,
                        @Value("${platform.appointment-queue.enabled:false}") boolean scheduleQueueEnabled,
                        @Value("${platform.appointment-queue.offer-timeout-ms:50}") long queueOfferTimeoutMillis) {
        this.appointmentApplicationService = appointmentApplicationService;
        this.appointmentApiMapper = appointmentApiMapper;
        this.scheduleQueue = scheduleQueue;
        this.scheduleQueueEnabled = scheduleQueueEnabled;
        this.queueOfferTimeoutMillis = queueOfferTimeoutMillis;

        Gauge.builder("platform.appointment.queue.depth", scheduleQueue, BlockingQueue::size)
            .description("Current depth of the appointment scheduling queue")
            .register(meterRegistry);

        this.scheduleQueueFullCounter = io.micrometer.core.instrument.Counter
            .builder("platform.appointment.schedule.queue_full")
            .description("Number of schedule requests rejected because the queue was full")
            .tags(Tags.empty())
            .register(meterRegistry);
    }

    @PostMapping
    public ResponseEntity<?> schedule(@RequestBody ScheduleAppointmentRequest request) {
        AppointmentApplicationService.ScheduleCommand command =
            appointmentApiMapper.toScheduleCommand(request);

        if (!scheduleQueueEnabled) {
            AppointmentEntity entity = appointmentApplicationService.schedule(command);
            return ResponseEntity.ok(appointmentApiMapper.toResponse(entity));
        }

        boolean accepted;
        try {
            accepted = scheduleQueue.offer(command, queueOfferTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Scheduling interrupted while waiting for queue capacity");
        }

        if (!accepted) {
            scheduleQueueFullCounter.increment();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Scheduling queue is full; please retry later");
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> complete(@PathVariable("id") Long id,
                                      @RequestBody CompleteAppointmentRequest request) {
        AppointmentApplicationService.CompleteCommand command =
            appointmentApiMapper.toCompleteCommand(id, request);

        Optional<AppointmentEntity> updated = appointmentApplicationService.complete(command);

        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AppointmentEntity entity = updated.get();

        return ResponseEntity.ok(appointmentApiMapper.toResponse(entity));
    }

    @GetMapping("/stats/by-scheduled-at")
    public ResponseEntity<?> statsByScheduledAt(@RequestParam("organizationId") Long organizationId,
                                                @RequestParam("clinicId") Long clinicId,
                                                @RequestParam("fromScheduledAtIso") String fromScheduledAtIso,
                                                @RequestParam(value = "toScheduledAtIso", required = false) String toScheduledAtIso) {

        Instant from = Instant.parse(fromScheduledAtIso);
        Instant to = toScheduledAtIso != null ? Instant.parse(toScheduledAtIso) : Instant.now();

        List<Long> appointmentIds = appointmentApplicationService
                .findAppointmentIdsScheduledBetweenForTenant(organizationId, clinicId, from, to);

        AppointmentStatsResponse response = new AppointmentStatsResponse(appointmentIds.size(), appointmentIds);

        return ResponseEntity.ok(response);
    }

    public record ScheduleAppointmentRequest(Long organizationId,
                                             Long clinicId,
                                             Long patientId,
                                             String modality,
                                             String scheduledAtIso) {
    }

    public record CompleteAppointmentRequest(String startedAtIso,
                                             String completedAtIso) {
    }

    public record AppointmentResponse(Long id,
                                      Long organizationId,
                                      Long clinicId,
                                      Long patientId,
                                      String modality,
                                      String status,
                                      Instant scheduledAt,
                                      Instant startedAt,
                                      Instant completedAt) {
    }

    public record AppointmentStatsResponse(long count,
                                           List<Long> appointmentIds) {
    }
}
