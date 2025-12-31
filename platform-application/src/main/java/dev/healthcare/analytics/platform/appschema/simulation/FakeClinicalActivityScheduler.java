package dev.healthcare.analytics.platform.appschema.simulation;

import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * FakeClinicalActivityScheduler simulates OLTP activity (profiles, appointments).
 *
 * It is intentionally small and focused:
 * - generates realistic-but-fake user and appointment events
 * - should call regular domain services which, in turn, publish to app.outbox_event
 */
@Component
public class FakeClinicalActivityScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FakeClinicalActivityScheduler.class);

    private final Faker faker = new Faker(Locale.ENGLISH);

    // In a full implementation, a domain service would be injected here.

    @Scheduled(fixedDelayString = "${platform.simulation.fixed-delay-ms:10000}")
    public void simulateClinicalActivityBurst() {
        // DRY + KISS: keep behaviour minimal until we need more complexity.
        int simulatedAppointments = faker.random().nextInt(1, 5);
        LOGGER.info("Simulating {} fake clinical appointment events (skeleton only)", simulatedAppointments);
    }
}
