package com.acme.coldtrace.platform.alerts.interfaces.rest.resources;

/**
 * REST resource representing one AI resolution plan step.
 *
 * @param sequence execution order
 * @param action operator-facing action
 * @param rationale reason for the action
 * @param expectedOutcome expected operational result
 * @since 1.0
 */
public record AiResolutionPlanStepResource(
        Integer sequence,
        String action,
        String rationale,
        String expectedOutcome
) {
}
