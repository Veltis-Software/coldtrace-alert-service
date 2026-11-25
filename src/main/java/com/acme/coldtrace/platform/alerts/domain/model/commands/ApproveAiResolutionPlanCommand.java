package com.acme.coldtrace.platform.alerts.domain.model.commands;

/**
 * Command for approving a pending AI resolution plan.
 *
 * @since 1.0
 */
public record ApproveAiResolutionPlanCommand(
        Long organizationId,
        Long incidentId,
        Long planId,
        String approvedBy,
        String finalCorrectiveAction,
        String finalResolutionNotes
) {
    /**
     * Validates and normalizes approval data.
     */
    public ApproveAiResolutionPlanCommand {
        organizationId = requirePositive(organizationId, "alerts.incident.error.organizationId.invalid");
        incidentId = requirePositive(incidentId, "alerts.incident.error.incidentId.invalid");
        planId = requirePositive(planId, "alerts.ai-resolution-plan.error.planId.invalid");
        approvedBy = requireNonBlank(approvedBy, "alerts.ai-resolution-plan.error.approvedBy.required");
        finalCorrectiveAction = requireNonBlank(
                finalCorrectiveAction,
                "alerts.ai-resolution-plan.error.finalCorrectiveAction.required"
        );
        finalResolutionNotes = requireNonBlank(
                finalResolutionNotes,
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
