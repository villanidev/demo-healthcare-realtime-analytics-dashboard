package dev.healthcare.analytics.platform.streampipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.healthcare.analytics.platform.analyticsschema.StreamCheckpoint;
import dev.healthcare.analytics.platform.analyticsschema.StreamCheckpointRepository;
import dev.healthcare.analytics.platform.analyticsschema.fact.AppointmentFunnelFact;
import dev.healthcare.analytics.platform.analyticsschema.repository.AppointmentFunnelFactRepository;
import dev.healthcare.analytics.platform.appschema.outbox.AppOutboxEvent;
import dev.healthcare.analytics.platform.appschema.outbox.AppOutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * OutboxEventStreamProcessor is the core Kappa stream processor.
 * It reads new events from the transactional app.outbox_event stream
 * and projects them into analytics.fact tables and aggregates.
 */
@Component
public class OutboxEventStreamProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxEventStreamProcessor.class);

    private static final String STREAM_NAME = "outbox_to_appointment_funnel";

    private final AppOutboxEventRepository outboxEventRepository;
    private final StreamCheckpointRepository checkpointRepository;
    private final AppointmentFunnelFactRepository appointmentFunnelFactRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventStreamProcessor(AppOutboxEventRepository outboxEventRepository,
                                      StreamCheckpointRepository checkpointRepository,
                                      AppointmentFunnelFactRepository appointmentFunnelFactRepository,
                                      ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.checkpointRepository = checkpointRepository;
        this.appointmentFunnelFactRepository = appointmentFunnelFactRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${platform.stream-pipeline.outbox-poll-interval-ms:1000}")
    public void pollAndProjectOutboxEvents() {
        StreamCheckpoint checkpoint = checkpointRepository.findById(STREAM_NAME)
                .orElseGet(() -> new StreamCheckpoint(STREAM_NAME, 0L));

        long lastProcessedId = checkpoint.getLastProcessedEventId();
        List<AppOutboxEvent> batch = outboxEventRepository.findTop200ByIdGreaterThanOrderByIdAsc(lastProcessedId);
        if (batch.isEmpty()) {
            return;
        }

        long highestSeenId = lastProcessedId;
        for (AppOutboxEvent event : batch) {
            highestSeenId = Math.max(highestSeenId, event.getId());
            projectOutboxEvent(event);
        }

        checkpoint.setLastProcessedEventId(highestSeenId);
        checkpointRepository.save(checkpoint);
        LOGGER.debug("Projected {} outbox events into analytics (checkpoint={})", batch.size(), highestSeenId);
    }

    private void projectOutboxEvent(AppOutboxEvent event) {
        try {
            JsonNode root = objectMapper.readTree(event.getEventPayload());
            String eventType = event.getEventType();

            if ("APPOINTMENT_SCHEDULED".equals(eventType)) {
                applyAppointmentScheduled(root);
            } else if ("APPOINTMENT_COMPLETED".equals(eventType)) {
                applyAppointmentCompleted(root);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to project outbox event id={} type={}", event.getId(), event.getEventType(), e);
        }
    }

    private void applyAppointmentScheduled(JsonNode root) {
        String appointmentId = asString(root, "appointmentId");
        String organizationId = asString(root, "organizationId");
        String clinicId = asString(root, "clinicId");
        String patientId = asString(root, "patientId");
        String modality = asString(root, "modality");
        Instant scheduledAt = asInstant(root, "scheduledAt");
        String status = asString(root, "status");

        AppointmentFunnelFact fact = appointmentFunnelFactRepository.findById(appointmentId)
                .orElseGet(AppointmentFunnelFact::new);

        fact.setAppointmentId(appointmentId);
        fact.setOrganizationId(organizationId);
        fact.setClinicId(clinicId);
        fact.setPatientId(patientId);
        fact.setModality(modality);
        fact.setScheduledAt(scheduledAt);
        fact.setStatus(status);

        appointmentFunnelFactRepository.save(fact);
    }

    private void applyAppointmentCompleted(JsonNode root) {
        String appointmentId = asString(root, "appointmentId");
        Instant scheduledAt = asInstant(root, "scheduledAt");
        Instant startedAt = asInstant(root, "startedAt");
        Instant completedAt = asInstant(root, "completedAt");
        String status = asString(root, "status");

        AppointmentFunnelFact fact = appointmentFunnelFactRepository.findById(appointmentId)
                .orElseGet(AppointmentFunnelFact::new);

        fact.setAppointmentId(appointmentId);
        if (fact.getScheduledAt() == null) {
            fact.setScheduledAt(scheduledAt);
        }
        fact.setStartedAt(startedAt);
        fact.setCompletedAt(completedAt);
        fact.setStatus(status);

        if (scheduledAt != null && startedAt != null) {
            fact.setScheduledToStartSeconds(Duration.between(scheduledAt, startedAt).getSeconds());
        }
        if (startedAt != null && completedAt != null) {
            fact.setStartToCompleteSeconds(Duration.between(startedAt, completedAt).getSeconds());
        }

        appointmentFunnelFactRepository.save(fact);
    }

    private String asString(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node != null && !node.isNull() ? node.asText() : null;
    }

    private Instant asInstant(JsonNode root, String field) {
        String value = asString(root, field);
        return value != null ? Instant.parse(value) : null;
    }
}
