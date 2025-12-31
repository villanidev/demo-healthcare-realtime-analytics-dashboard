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

    public AppointmentFunnelFact() {
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getClinicId() {
        return clinicId;
    }

    public void setClinicId(String clinicId) {
        this.clinicId = clinicId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getModality() {
        return modality;
    }

    public void setModality(String modality) {
        this.modality = modality;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getScheduledToStartSeconds() {
        return scheduledToStartSeconds;
    }

    public void setScheduledToStartSeconds(Long scheduledToStartSeconds) {
        this.scheduledToStartSeconds = scheduledToStartSeconds;
    }

    public Long getStartToCompleteSeconds() {
        return startToCompleteSeconds;
    }

    public void setStartToCompleteSeconds(Long startToCompleteSeconds) {
        this.startToCompleteSeconds = startToCompleteSeconds;
    }
}
