package com.acme.coldtrace.platform.alerts.domain.repositories;

import com.acme.coldtrace.platform.alerts.domain.model.aggregates.AiResolutionPlan;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository contract for AI resolution plan audit records.
 *
 * @since 1.0
 */
public interface AiResolutionPlanRepository {
    List<AiResolutionPlan> findAllByIncidentIdAndOrganizationId(Long incidentId, Long organizationId);

    Optional<AiResolutionPlan> findByIdAndIncidentIdAndOrganizationId(
            Long id,
            Long incidentId,
            Long organizationId
    );

    AiResolutionPlan save(AiResolutionPlan plan);
}
