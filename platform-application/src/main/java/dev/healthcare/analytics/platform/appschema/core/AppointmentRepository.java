package dev.healthcare.analytics.platform.appschema.core;

import dev.healthcare.analytics.platform.domain.appointment.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long> {

	List<AppointmentEntity> findByOrganization_IdAndClinic_IdAndScheduledAtBetween(Long organizationId,
											   Long clinicId,
											   Instant from,
											   Instant to);

	List<AppointmentEntity> findByOrganization_IdAndClinic_IdAndStatusAndScheduledAtBetween(Long organizationId,
													   Long clinicId,
													   AppointmentStatus status,
													   Instant from,
													   Instant to);
}
