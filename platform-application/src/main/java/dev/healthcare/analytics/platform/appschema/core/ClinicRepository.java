package dev.healthcare.analytics.platform.appschema.core;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicRepository extends JpaRepository<ClinicEntity, Long> {
}
