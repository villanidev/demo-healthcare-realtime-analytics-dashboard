package dev.healthcare.analytics.platform.api;

import dev.healthcare.analytics.platform.appschema.core.AppointmentEntity;
import dev.healthcare.analytics.platform.domain.appointment.AppointmentApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentApiController {

    private final AppointmentApplicationService appointmentApplicationService;
    private final AppointmentApiMapper appointmentApiMapper;

    public AppointmentApiController(AppointmentApplicationService appointmentApplicationService) {
        this.appointmentApplicationService = appointmentApplicationService;
        this.appointmentApiMapper = new AppointmentApiMapper();
    }

    @PostMapping
    public ResponseEntity<?> schedule(@RequestBody ScheduleAppointmentRequest request) {
        AppointmentApplicationService.ScheduleCommand command =
            appointmentApiMapper.toScheduleCommand(request);

        AppointmentEntity entity = appointmentApplicationService.schedule(command);

        return ResponseEntity.ok(appointmentApiMapper.toResponse(entity));
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
}
