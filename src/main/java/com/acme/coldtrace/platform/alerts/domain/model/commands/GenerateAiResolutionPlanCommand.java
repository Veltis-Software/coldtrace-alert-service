package com.acme.coldtrace.platform.alerts.domain.model.commands;

/**
 * Command for generating and persisting an AI resolution plan from backend context.
 *
 * @param organizationId organization identifier
 * @param incidentId incident identifier
 * @since 1.0
 */
public record GenerateAiResolutionPlanCommand(Long organizationId, Long incidentId) {
}
