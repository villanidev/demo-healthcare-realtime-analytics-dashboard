package dev.healthcare.analytics.platform.analyticsschema.service;

import dev.healthcare.analytics.platform.analyticsschema.fact.AppointmentFunnelFact;
import dev.healthcare.analytics.platform.analyticsschema.repository.AppointmentFunnelFactRepository;
import dev.healthcare.analytics.platform.analyticsschema.view.AppointmentFunnelSnapshot;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AppointmentFunnelReadService {

    private final AppointmentFunnelFactRepository factRepository;

    public AppointmentFunnelReadService(AppointmentFunnelFactRepository factRepository) {
        this.factRepository = factRepository;
    }

    public AppointmentFunnelSnapshot loadSnapshot(String organizationId, String clinicId) {
        List<AppointmentFunnelFact> facts = factRepository.findByOrganizationIdAndClinicId(organizationId, clinicId);

        AppointmentFunnelSnapshot snapshot = new AppointmentFunnelSnapshot();
        snapshot.setOrganizationId(organizationId);
        snapshot.setClinicId(clinicId);

        if (facts.isEmpty()) {
            snapshot.setLastUpdatedAt(Instant.now());
            return snapshot;
        }

        long scheduled = 0;
        long completed = 0;
        long virtualCount = 0;
        long inPersonCount = 0;

        long totalScheduledToStart = 0;
        long totalStartToComplete = 0;
        long countScheduledToStart = 0;
        long countStartToComplete = 0;

        Instant maxCompletedAt = null;

        for (AppointmentFunnelFact fact : facts) {
            if ("SCHEDULED".equals(fact.getStatus())) {
                scheduled++;
            } else if ("COMPLETED".equals(fact.getStatus())) {
                completed++;
            }

            if ("VIRTUAL".equalsIgnoreCase(fact.getModality())) {
                virtualCount++;
            } else if ("IN_PERSON".equalsIgnoreCase(fact.getModality())) {
                inPersonCount++;
            }

            if (fact.getScheduledToStartSeconds() != null) {
                totalScheduledToStart += fact.getScheduledToStartSeconds();
                countScheduledToStart++;
            }

            if (fact.getStartToCompleteSeconds() != null) {
                totalStartToComplete += fact.getStartToCompleteSeconds();
                countStartToComplete++;
            }

            if (fact.getCompletedAt() != null) {
                if (maxCompletedAt == null || fact.getCompletedAt().isAfter(maxCompletedAt)) {
                    maxCompletedAt = fact.getCompletedAt();
                }
            }
        }

        snapshot.setScheduledCount(scheduled);
        snapshot.setCompletedCount(completed);
        snapshot.setVirtualCount(virtualCount);
        snapshot.setInPersonCount(inPersonCount);

        if (countScheduledToStart > 0) {
            snapshot.setAverageScheduledToStartSeconds(totalScheduledToStart / (double) countScheduledToStart);
        }

        if (countStartToComplete > 0) {
            snapshot.setAverageStartToCompleteSeconds(totalStartToComplete / (double) countStartToComplete);
        }

        snapshot.setLastUpdatedAt(maxCompletedAt != null ? maxCompletedAt : Instant.now());

        return snapshot;
    }
}
