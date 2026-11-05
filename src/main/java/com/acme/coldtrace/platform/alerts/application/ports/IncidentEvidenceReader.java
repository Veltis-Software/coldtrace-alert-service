package com.acme.coldtrace.platform.alerts.application.ports;
import java.util.Optional;
public interface IncidentEvidenceReader {
 Optional<IncidentEvidenceSnapshot> findByIdAndOrganizationId(Long id, Long organizationId);
 boolean existsByIdAndOrganizationId(Long id, Long organizationId);
}
