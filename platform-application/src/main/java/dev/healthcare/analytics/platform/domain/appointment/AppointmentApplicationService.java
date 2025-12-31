package dev.healthcare.analytics.platform.domain.appointment;

import dev.healthcare.analytics.platform.appschema.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class AppointmentApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentApplicationService.class);

    private final AppointmentCommandService appointmentCommandService;
    private final OrganizationRepository organizationRepository;
    private final ClinicRepository clinicRepository;
    private final PatientAccountRepository patientAccountRepository;
    private final AppointmentRepository appointmentRepository;

    public AppointmentApplicationService(AppointmentCommandService appointmentCommandService,
                                         OrganizationRepository organizationRepository,
                                         ClinicRepository clinicRepository,
                                         PatientAccountRepository patientAccountRepository,
                                         AppointmentRepository appointmentRepository) {
        this.appointmentCommandService = appointmentCommandService;
        this.organizationRepository = organizationRepository;
        this.clinicRepository = clinicRepository;
        this.patientAccountRepository = patientAccountRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public AppointmentEntity schedule(ScheduleCommand command) {
        OrganizationEntity organization = organizationRepository.findById(command.organizationId())
            .orElseThrow(() -> new IllegalArgumentException("Invalid organization id"));
        ClinicEntity clinic = clinicRepository.findById(command.clinicId())
            .orElseThrow(() -> new IllegalArgumentException("Invalid clinic id"));
        PatientAccountEntity patient = patientAccountRepository.findById(command.patientId())
            .orElseThrow(() -> new IllegalArgumentException("Invalid patient id"));

        AppointmentEntity entity = appointmentCommandService.scheduleAppointment(
            organization,
            clinic,
            patient,
            command.modality(),
            command.scheduledAt()
        );

        LOGGER.debug("Scheduled appointment id={} org={} clinic={} patient={} modality={} at={}",
                entity.getId(), organization.getId(), clinic.getId(), patient.getId(),
                command.modality(), command.scheduledAt());

        return entity;
    }

    public Optional<AppointmentEntity> complete(CompleteCommand command) {
        return appointmentRepository.findById(command.appointmentId())
            .map(existing -> {
                appointmentCommandService.completeAppointment(
                    existing,
                    command.startedAt(),
                    command.completedAt()
                );
                // reload to reflect latest state
                AppointmentEntity updated = appointmentRepository.findById(existing.getId()).orElse(existing);

                LOGGER.debug("Completed appointment id={} org={} clinic={} patient={} startedAt={} completedAt={}",
                        updated.getId(),
                        updated.getOrganization().getId(),
                        updated.getClinic().getId(),
                        updated.getPatientAccount().getId(),
                        updated.getStartedAt(),
                        updated.getCompletedAt());

                return updated;
            });
    }

    public record ScheduleCommand(Long organizationId,
                                  Long clinicId,
                                  Long patientId,
                                  AppointmentModality modality,
                                  Instant scheduledAt) {
    }

    public record CompleteCommand(Long appointmentId,
                                  Instant startedAt,
                                  Instant completedAt) {
    }
}
