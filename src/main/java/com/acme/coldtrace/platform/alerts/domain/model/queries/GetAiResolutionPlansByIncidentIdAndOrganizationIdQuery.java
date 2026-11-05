package com.acme.coldtrace.platform.alerts.domain.model.queries;

/**
 * Query for retrieving AI resolution plan history for one incident.
 *
 * @since 1.0
 */
public record GetAiResolutionPlansByIncidentIdAndOrganizationIdQuery(Long organizationId, Long incidentId) {
}
