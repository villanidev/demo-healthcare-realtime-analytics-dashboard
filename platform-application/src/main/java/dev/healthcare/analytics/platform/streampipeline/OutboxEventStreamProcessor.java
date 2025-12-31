package dev.healthcare.analytics.platform.streampipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * OutboxEventStreamProcessor is the core Kappa stream processor.
 * It reads new events from the transactional app.outbox_event stream
 * and projects them into analytics.fact tables and aggregates.
 */
@Component
public class OutboxEventStreamProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxEventStreamProcessor.class);

    // In a complete implementation, repositories and a checkpoint store would be injected here.

    @Scheduled(fixedDelayString = "${platform.stream-pipeline.outbox-poll-interval-ms:1000}")
    public void pollAndProjectOutboxEvents() {
        // KISS + YAGNI: keep POC behavior minimal and focused.
        // Later we can add idempotency and checkpointing while preserving this simple contract.
        LOGGER.debug("Polling app.outbox_event stream for new domain events...");
    }
}
