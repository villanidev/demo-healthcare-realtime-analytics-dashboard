package dev.healthcare.analytics.platform.analyticsschema.fact;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "appointment_funnel_fact", schema = "analytics")
public class AppointmentFunnelFact {

    @Id
    @Column(name = "appointment_id", nullable = false, length = 64)
    private String appointmentId;

    @Column(name = "organization_id", nullable = false, length = 64)
    private String organizationId;

    @Column(name = "clinic_id", nullable = false, length = 64)
    private String clinicId;

    @Column(name = "patient_id", nullable = false, length = 64)
    private String patientId;

    @Column(name = "modality", nullable = false, length = 32)
    private String modality;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    // Derived latency metrics (optional but explicit naming)
    @Column(name = "scheduled_to_start_seconds")
    private Long scheduledToStartSeconds;

    @Column(name = "start_to_complete_seconds")
    private Long startToCompleteSeconds;

    // Getters and setters omitted for brevity in this POC skeleton
}
