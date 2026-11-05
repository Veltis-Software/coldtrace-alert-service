package com.acme.coldtrace.platform.alerts.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request resource used to reject a pending AI resolution plan.
 *
 * @param rejectedBy actor that rejects the AI plan
 * @param rejectionReason audit reason for rejecting the plan
 * @since 1.0
 */
@Schema(
        name = "RejectAiResolutionPlanRequest",
        description = "Request payload for rejecting an AI plan without changing incident state"
)
public record RejectAiResolutionPlanResource(
        @NotBlank(message = "is required")
        @Schema(description = "Actor that rejects the plan", example = "operations.manager@coldtrace.test")
        String rejectedBy,

        @NotBlank(message = "is required")
        @Schema(description = "Reason recorded for audit history",
                example = "Plan requires on-site compressor inspection before closure.")
        String rejectionReason
) {
}
