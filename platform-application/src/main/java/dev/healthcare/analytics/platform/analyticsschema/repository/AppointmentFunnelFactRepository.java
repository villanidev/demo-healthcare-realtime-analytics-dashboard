package dev.healthcare.analytics.platform.analyticsschema.repository;

import dev.healthcare.analytics.platform.analyticsschema.fact.AppointmentFunnelFact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentFunnelFactRepository extends JpaRepository<AppointmentFunnelFact, String> {

    List<AppointmentFunnelFact> findByOrganizationIdAndClinicId(String organizationId, String clinicId);
}
