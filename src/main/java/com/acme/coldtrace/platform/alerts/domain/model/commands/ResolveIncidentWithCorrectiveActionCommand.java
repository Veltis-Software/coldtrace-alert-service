package com.acme.coldtrace.platform.alerts.domain.model.commands;

/**
 * Command for resolving an incident while storing the operator-approved corrective action.
 *
 * @param organizationId organization identifier
 * @param incidentId incident identifier
 * @param resolvedBy actor that approved and resolved the incident
 * @param correctiveAction final corrective action approved by the operator
 * @param resolutionNotes final resolution notes approved by the operator
 * @since 1.0
 */
public record ResolveIncidentWithCorrectiveActionCommand(
        Long organizationId,
        Long incidentId,
        String resolvedBy,
        String correctiveAction,
        String resolutionNotes
) {
    /**
     * Validates and normalizes resolution data.
     */
    public ResolveIncidentWithCorrectiveActionCommand {
        organizationId = requirePositive(organizationId, "alerts.incident.error.organizationId.invalid");
        incidentId = requirePositive(incidentId, "alerts.incident.error.incidentId.invalid");
        resolvedBy = requireNonBlank(resolvedBy, "alerts.incident.error.resolvedBy.required");
        correctiveAction = requireNonBlank(correctiveAction, "alerts.incident.error.correctiveAction.required");
        resolutionNotes = requireNonBlank(
                resolutionNotes,
                "alerts.ai-resolution-plan.error.finalResolutionNotes.required"
        );
    }

    private static Long requirePositive(Long value, String messageKey) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(messageKey);
        }
        return value;
    }

    private static String requireNonBlank(String value, String messageKey) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(messageKey);
        }
        return value.trim();
    }
}
