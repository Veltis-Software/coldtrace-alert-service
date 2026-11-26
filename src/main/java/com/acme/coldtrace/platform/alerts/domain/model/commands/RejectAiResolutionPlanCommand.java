package com.acme.coldtrace.platform.alerts.domain.model.commands;

/**
 * Command for rejecting a pending AI resolution plan.
 *
 * @since 1.0
 */
public record RejectAiResolutionPlanCommand(
        Long organizationId,
        Long incidentId,
        Long planId,
        String rejectedBy,
        String rejectionReason
) {
    /**
     * Validates and normalizes rejection data.
     */
    public RejectAiResolutionPlanCommand {
        organizationId = requirePositive(organizationId, "alerts.incident.error.organizationId.invalid");
        incidentId = requirePositive(incidentId, "alerts.incident.error.incidentId.invalid");
        planId = requirePositive(planId, "alerts.ai-resolution-plan.error.planId.invalid");
        rejectedBy = requireNonBlank(rejectedBy, "alerts.ai-resolution-plan.error.rejectedBy.required");
        rejectionReason = requireNonBlank(
                rejectionReason,
                "alerts.ai-resolution-plan.error.rejectionReason.required"
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
