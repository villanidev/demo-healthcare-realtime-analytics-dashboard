package dev.healthcare.analytics.platform.appschema.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class OutboxEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final AppOutboxEventRepository appOutboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(AppOutboxEventRepository appOutboxEventRepository, ObjectMapper objectMapper) {
        this.appOutboxEventRepository = appOutboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void publishEvent(String aggregateType,
                             String aggregateId,
                             String eventType,
                             Map<String, Object> eventPayload,
                             Instant eventTime) {
        String json;
        try {
            json = objectMapper.writeValueAsString(eventPayload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload for type " + eventType, e);
        }

        AppOutboxEvent event = new AppOutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setEventPayload(json);
        event.setEventTime(eventTime);
        event.setCreatedAt(Instant.now());

        appOutboxEventRepository.save(event);
        LOGGER.debug("Published outbox event: type={} aggregateType={} aggregateId={}", eventType, aggregateType, aggregateId);
    }
}
