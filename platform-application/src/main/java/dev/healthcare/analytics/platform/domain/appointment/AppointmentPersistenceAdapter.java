package dev.healthcare.analytics.platform.domain.appointment;

import dev.healthcare.analytics.platform.appschema.core.AppointmentEntity;
import dev.healthcare.analytics.platform.appschema.core.ClinicEntity;
import dev.healthcare.analytics.platform.appschema.core.OrganizationEntity;
import dev.healthcare.analytics.platform.appschema.core.PatientAccountEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AppointmentPersistenceAdapter {

    @PersistenceContext
    private EntityManager entityManager;

    public AppointmentEntity createScheduledAppointment(OrganizationEntity organization,
                                                        ClinicEntity clinic,
                                                        PatientAccountEntity patient,
                                                        AppointmentModality modality,
                                                        Instant scheduledAt) {
        AppointmentEntity entity = new AppointmentEntity();
        entity.setOrganization(organization);
        entity.setClinic(clinic);
        entity.setPatientAccount(patient);
        entity.setModality(modality);
        entity.setStatus(AppointmentStatus.SCHEDULED);
        entity.setScheduledAt(scheduledAt);

        entityManager.persist(entity);
        return entity;
    }

    public void markAppointmentCompleted(AppointmentEntity appointment,
                                          Instant startedAt,
                                          Instant completedAt) {
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setStartedAt(startedAt);
        appointment.setCompletedAt(completedAt);
        entityManager.merge(appointment);
    }
}
