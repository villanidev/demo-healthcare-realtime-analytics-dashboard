package dev.healthcare.analytics.platform.api;

import dev.healthcare.analytics.platform.appschema.core.AppointmentEntity;
import dev.healthcare.analytics.platform.domain.appointment.AppointmentApplicationService;
import dev.healthcare.analytics.platform.domain.appointment.AppointmentModality;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AppointmentApiMapper {

    public AppointmentApplicationService.ScheduleCommand toScheduleCommand(AppointmentApiController.ScheduleAppointmentRequest request) {
        if (request.modality() == null) {
            throw new IllegalArgumentException("modality is required (VIRTUAL or IN_PERSON)");
        }

        AppointmentModality modality;
        try {
            modality = AppointmentModality.valueOf(request.modality());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid modality; expected VIRTUAL or IN_PERSON");
        }

        Instant scheduledAt;
        if (request.scheduledAtIso() != null) {
            scheduledAt = Instant.parse(request.scheduledAtIso());
        } else {
            scheduledAt = Instant.now();
        }

        return new AppointmentApplicationService.ScheduleCommand(
            request.organizationId(),
            request.clinicId(),
            request.patientId(),
            modality,
            scheduledAt
        );
    }

    public AppointmentApplicationService.CompleteCommand toCompleteCommand(Long id, AppointmentApiController.CompleteAppointmentRequest request) {
        Instant startedAt;
        if (request.startedAtIso() != null) {
            startedAt = Instant.parse(request.startedAtIso());
        } else {
            startedAt = Instant.now();
        }

        Instant completedAt;
        if (request.completedAtIso() != null) {
            completedAt = Instant.parse(request.completedAtIso());
        } else {
            completedAt = startedAt.plusSeconds(15 * 60);
        }

        return new AppointmentApplicationService.CompleteCommand(
            id,
            startedAt,
            completedAt
        );
    }

    public AppointmentApiController.AppointmentResponse toResponse(AppointmentEntity entity) {
        return new AppointmentApiController.AppointmentResponse(
            entity.getId(),
            entity.getOrganization().getId(),
            entity.getClinic().getId(),
            entity.getPatientAccount().getId(),
            entity.getModality().name(),
            entity.getStatus().name(),
            entity.getScheduledAt(),
            entity.getStartedAt(),
            entity.getCompletedAt()
        );
    }
}
