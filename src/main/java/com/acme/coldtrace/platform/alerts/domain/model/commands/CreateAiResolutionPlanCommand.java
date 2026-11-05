package com.acme.coldtrace.platform.alerts.domain.model.commands;

import com.acme.coldtrace.platform.alerts.domain.model.valueobjects.AiResolutionPlanStep;

import java.util.List;

/**
 * Command for persisting a newly generated AI resolution plan as pending.
 *
 * @since 1.0
 */
public record CreateAiResolutionPlanCommand(
        Long organizationId,
        Long incidentId,
        String summary,
        String probableCause,
        List<AiResolutionPlanStep> recommendedSteps,
        String correctiveActionDraft,
        String resolutionNotesDraft,
        Boolean escalationRecommended,
        String escalationUrgency,
        String escalationReason,
        List<String> requiredEvidence,
        List<String> uncertaintyNotes,
        String modelProvider,
        String modelName,
        String providerMetadata
) {
    public CreateAiResolutionPlanCommand {
        recommendedSteps = recommendedSteps == null ? List.of() : List.copyOf(recommendedSteps);
        requiredEvidence = requiredEvidence == null ? List.of() : List.copyOf(requiredEvidence);
        uncertaintyNotes = uncertaintyNotes == null ? List.of() : List.copyOf(uncertaintyNotes);
    }
}
