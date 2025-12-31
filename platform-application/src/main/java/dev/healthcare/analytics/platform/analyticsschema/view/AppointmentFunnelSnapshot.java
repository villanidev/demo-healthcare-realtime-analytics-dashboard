package dev.healthcare.analytics.platform.analyticsschema.view;

import java.time.Instant;

/**
 * AppointmentFunnelSnapshot is a small read model used by the dashboard.
 * It aggregates appointment funnel and modality metrics per tenant.
 */
public class AppointmentFunnelSnapshot {

    private String organizationId;
    private String clinicId;

    private long scheduledCount;
    private long completedCount;

    private long virtualCount;
    private long inPersonCount;

    private Double averageScheduledToStartSeconds;
    private Double averageStartToCompleteSeconds;

    private Instant lastUpdatedAt;

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

    public long getScheduledCount() {
        return scheduledCount;
    }

    public void setScheduledCount(long scheduledCount) {
        this.scheduledCount = scheduledCount;
    }

    public long getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(long completedCount) {
        this.completedCount = completedCount;
    }

    public long getVirtualCount() {
        return virtualCount;
    }

    public void setVirtualCount(long virtualCount) {
        this.virtualCount = virtualCount;
    }

    public long getInPersonCount() {
        return inPersonCount;
    }

    public void setInPersonCount(long inPersonCount) {
        this.inPersonCount = inPersonCount;
    }

    public Double getAverageScheduledToStartSeconds() {
        return averageScheduledToStartSeconds;
    }

    public void setAverageScheduledToStartSeconds(Double averageScheduledToStartSeconds) {
        this.averageScheduledToStartSeconds = averageScheduledToStartSeconds;
    }

    public Double getAverageStartToCompleteSeconds() {
        return averageStartToCompleteSeconds;
    }

    public void setAverageStartToCompleteSeconds(Double averageStartToCompleteSeconds) {
        this.averageStartToCompleteSeconds = averageStartToCompleteSeconds;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
