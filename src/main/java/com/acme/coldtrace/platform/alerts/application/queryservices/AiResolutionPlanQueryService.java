package com.acme.coldtrace.platform.alerts.application.queryservices;

import com.acme.coldtrace.platform.alerts.domain.model.aggregates.AiResolutionPlan;
import com.acme.coldtrace.platform.alerts.domain.model.queries.GetAiResolutionPlansByIncidentIdAndOrganizationIdQuery;
import com.acme.coldtrace.platform.shared.application.result.Result;

import java.util.List;

/**
 * Application service contract for AI resolution plan history queries.
 *
 * @since 1.0
 */
public interface AiResolutionPlanQueryService {
    /**
     * Retrieves AI plan history for one organization-scoped incident.
     *
     * @param query query object containing organization and incident identifiers
     * @return success with plan history or failure with query error
     */
    Result<List<AiResolutionPlan>, AiResolutionPlanQueryFailure> handle(
            GetAiResolutionPlansByIncidentIdAndOrganizationIdQuery query
    );
}
