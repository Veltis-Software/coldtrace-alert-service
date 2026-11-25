package com.acme.coldtrace.platform.alerts.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request resource used to approve a pending AI resolution plan.
 *
 * @param approvedBy actor that approves the AI plan
 * @param finalCorrectiveAction final corrective action selected by the operator
 * @param finalResolutionNotes final notes used to resolve the incident
 * @since 1.0
 */
@Schema(
        name = "ApproveAiResolutionPlanRequest",
        description = "Request payload for approving an AI plan and resolving the incident"
)
public record ApproveAiResolutionPlanResource(
        @NotBlank(message = "is required")
        @Schema(description = "Actor that approves the plan", example = "operations.manager@coldtrace.test")
        String approvedBy,

        @NotBlank(message = "is required")
        @Schema(description = "Final operator-approved corrective action",
                example = "Moved inventory to backup freezer and recalibrated the affected sensor")
        String finalCorrectiveAction,

        @NotBlank(message = "is required")
        @Schema(description = "Final resolution notes",
                example = "Temperature returned to safe range after transfer and recalibration.")
        String finalResolutionNotes
) {
}
