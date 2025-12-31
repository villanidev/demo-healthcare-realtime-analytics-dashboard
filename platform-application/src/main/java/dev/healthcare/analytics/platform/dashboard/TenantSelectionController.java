package dev.healthcare.analytics.platform.dashboard;

import dev.healthcare.analytics.platform.appschema.core.ClinicEntity;
import dev.healthcare.analytics.platform.appschema.core.ClinicRepository;
import dev.healthcare.analytics.platform.appschema.core.OrganizationEntity;
import dev.healthcare.analytics.platform.appschema.core.OrganizationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tenants")
public class TenantSelectionController {

    private final OrganizationRepository organizationRepository;
    private final ClinicRepository clinicRepository;

    public TenantSelectionController(OrganizationRepository organizationRepository,
                                     ClinicRepository clinicRepository) {
        this.organizationRepository = organizationRepository;
        this.clinicRepository = clinicRepository;
    }

    @GetMapping("/default")
    public ResponseEntity<PrimaryTenantSelection> defaultTenant() {
        Optional<OrganizationEntity> organization = organizationRepository.findAll().stream().findFirst();
        Optional<ClinicEntity> clinic = clinicRepository.findAll().stream().findFirst();

        if (organization.isEmpty() || clinic.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        OrganizationEntity org = organization.get();
        ClinicEntity cl = clinic.get();

        PrimaryTenantSelection selection = new PrimaryTenantSelection(
            org.getId(),
            org.getName(),
            cl.getId(),
            cl.getName()
        );

        return ResponseEntity.ok(selection);
    }

    @GetMapping("/clinics")
    public List<PrimaryTenantSelection> allClinics() {
        return clinicRepository.findAll().stream()
            .map(clinic -> {
                OrganizationEntity org = clinic.getOrganization();
                return new PrimaryTenantSelection(
                    org != null ? org.getId() : null,
                    org != null ? org.getName() : null,
                    clinic.getId(),
                    clinic.getName()
                );
            })
            .toList();
    }

    public record PrimaryTenantSelection(Long organizationId,
                                         String organizationName,
                                         Long clinicId,
                                         String clinicName) {
    }
}
