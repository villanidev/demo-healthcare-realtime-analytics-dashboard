package dev.healthcare.analytics.platform.domain.appointment;

import dev.healthcare.analytics.platform.appschema.core.AppointmentEntity;
import dev.healthcare.analytics.platform.appschema.core.ClinicEntity;
import dev.healthcare.analytics.platform.appschema.core.OrganizationEntity;
import dev.healthcare.analytics.platform.appschema.core.PatientAccountEntity;
import dev.healthcare.analytics.platform.appschema.outbox.OutboxEventPublisher;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class AppointmentCommandService {

    private final AppointmentPersistenceAdapter appointmentPersistenceAdapter;
    private final OutboxEventPublisher outboxEventPublisher;

    public AppointmentCommandService(AppointmentPersistenceAdapter appointmentPersistenceAdapter,
                                     OutboxEventPublisher outboxEventPublisher) {
        this.appointmentPersistenceAdapter = appointmentPersistenceAdapter;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional
    public AppointmentEntity scheduleAppointment(OrganizationEntity organization,
                                                 ClinicEntity clinic,
                                                 PatientAccountEntity patient,
                                                 AppointmentModality modality,
                                                 Instant scheduledAt) {
        AppointmentEntity entity = appointmentPersistenceAdapter.createScheduledAppointment(organization, clinic, patient, modality, scheduledAt);

        Map<String, Object> payload = new HashMap<>();
        payload.put("appointmentId", entity.getId());
        payload.put("organizationId", organization.getId());
        payload.put("clinicId", clinic.getId());
        payload.put("patientId", patient.getId());
        payload.put("modality", modality.name());
        payload.put("scheduledAt", scheduledAt);
        payload.put("status", AppointmentStatus.SCHEDULED.name());

        outboxEventPublisher.publishEvent("APPOINTMENT", String.valueOf(entity.getId()),
                "APPOINTMENT_SCHEDULED", payload, scheduledAt);

        return entity;
    }

    @Transactional
    public void completeAppointment(AppointmentEntity appointment, Instant startedAt, Instant completedAt) {
        appointmentPersistenceAdapter.markAppointmentCompleted(appointment, startedAt, completedAt);

        Map<String, Object> payload = new HashMap<>();
        payload.put("appointmentId", appointment.getId());
        payload.put("organizationId", appointment.getOrganization().getId());
        payload.put("clinicId", appointment.getClinic().getId());
        payload.put("patientId", appointment.getPatientAccount().getId());
        payload.put("modality", appointment.getModality().name());
        payload.put("scheduledAt", appointment.getScheduledAt());
        payload.put("startedAt", startedAt);
        payload.put("completedAt", completedAt);
        payload.put("status", AppointmentStatus.COMPLETED.name());

        outboxEventPublisher.publishEvent("APPOINTMENT", String.valueOf(appointment.getId()),
                "APPOINTMENT_COMPLETED", payload, completedAt);
    }
}
