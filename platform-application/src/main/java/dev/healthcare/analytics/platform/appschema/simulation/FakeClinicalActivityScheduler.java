package dev.healthcare.analytics.platform.appschema.simulation;

import dev.healthcare.analytics.platform.appschema.core.ClinicEntity;
import dev.healthcare.analytics.platform.appschema.core.OrganizationEntity;
import dev.healthcare.analytics.platform.appschema.core.PatientAccountEntity;
import dev.healthcare.analytics.platform.domain.appointment.AppointmentCommandService;
import dev.healthcare.analytics.platform.domain.appointment.AppointmentModality;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    private final AppointmentCommandService appointmentCommandService;

    public FakeClinicalActivityScheduler(AppointmentCommandService appointmentCommandService) {
        this.appointmentCommandService = appointmentCommandService;
    }

    @Scheduled(fixedDelayString = "${platform.simulation.fixed-delay-ms:10000}")
    public void simulateClinicalActivityBurst() {
        // DRY + KISS: keep behaviour minimal until we need more complexity.
        int simulatedAppointments = faker.random().nextInt(1, 3);

        // For the POC we keep tenant identities simple and stable.
        OrganizationEntity org = new OrganizationEntity();
        ClinicEntity clinic = new ClinicEntity();
        clinic.setOrganization(org);
        PatientAccountEntity patient = new PatientAccountEntity();
        patient.setOrganization(org);
        patient.setClinic(clinic);
        patient.setDisplayName(faker.name().fullName());

        for (int i = 0; i < simulatedAppointments; i++) {
            AppointmentModality modality = faker.bool().bool() ? AppointmentModality.VIRTUAL : AppointmentModality.IN_PERSON;
            Instant scheduledAt = Instant.now();
            var appointment = appointmentCommandService.scheduleAppointment(org, clinic, patient, modality, scheduledAt);

            // Complete some of the appointments quickly to generate funnel metrics
            if (faker.bool().bool()) {
                Instant startedAt = scheduledAt.plus(faker.random().nextInt(1, 5), ChronoUnit.MINUTES);
                Instant completedAt = startedAt.plus(faker.random().nextInt(5, 20), ChronoUnit.MINUTES);
                appointmentCommandService.completeAppointment(appointment, startedAt, completedAt);
            }
        }

        LOGGER.info("Simulated {} fake clinical appointment events (scheduled/completed)", simulatedAppointments);
    }
}
