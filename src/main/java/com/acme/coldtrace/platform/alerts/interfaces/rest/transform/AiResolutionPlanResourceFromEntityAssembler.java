package com.acme.coldtrace.platform.alerts.interfaces.rest.transform;

import com.acme.coldtrace.platform.alerts.domain.model.aggregates.AiResolutionPlan;
import com.acme.coldtrace.platform.alerts.domain.model.valueobjects.AiResolutionPlanStep;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.AiResolutionPlanResource;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.AiResolutionPlanStepResource;

/**
 * Interface layer translator converting AI resolution plan entities to resources.
 *
 * @since 1.0
 */
public class AiResolutionPlanResourceFromEntityAssembler {
    /**
     * Converts an AI resolution plan aggregate into a REST resource.
     *
     * @param plan AI resolution plan aggregate
     * @return AI resolution plan resource
     */
    public static AiResolutionPlanResource toResourceFromEntity(AiResolutionPlan plan) {
        return new AiResolutionPlanResource(
                plan.getId(),
                plan.getOrganizationId(),
                plan.getIncidentId(),
                plan.getStatus().name().toLowerCase(),
                plan.getSummary(),
                plan.getProbableCause(),
                plan.getRecommendedSteps().stream()
                        .map(AiResolutionPlanResourceFromEntityAssembler::toStepResource)
                        .toList(),
                plan.getCorrectiveActionDraft(),
                plan.getResolutionNotesDraft(),
                plan.getEscalationRecommended(),
                plan.getEscalationUrgency(),
                plan.getEscalationReason(),
                plan.getRequiredEvidence(),
                plan.getUncertaintyNotes(),
                plan.getModelProvider(),
                plan.getModelName(),
                plan.getProviderMetadata(),
                plan.getGeneratedAt(),
                plan.getApprovedAt(),
                plan.getApprovedBy(),
                plan.getRejectedAt(),
                plan.getRejectedBy(),
                plan.getRejectionReason(),
                plan.getFinalCorrectiveAction(),
                plan.getFinalResolutionNotes()
        );
    }

    private static AiResolutionPlanStepResource toStepResource(AiResolutionPlanStep step) {
        return new AiResolutionPlanStepResource(
                step.getSequence(),
                step.getAction(),
                step.getRationale(),
                step.getExpectedOutcome()
        );
    }
}
