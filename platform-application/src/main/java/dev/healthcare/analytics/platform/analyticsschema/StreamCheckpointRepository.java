package dev.healthcare.analytics.platform.analyticsschema;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StreamCheckpointRepository extends JpaRepository<StreamCheckpoint, String> {
}
