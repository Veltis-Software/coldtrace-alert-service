package com.acme.coldtrace.platform.alerts.interfaces.rest.resources;

import java.time.Instant;
import java.util.List;

/**
 * REST resource representing one AI resolution plan audit record.
 *
 * @param id plan identifier
 * @param organizationId organization identifier
 * @param incidentId incident identifier
 * @param status plan lifecycle status
 * @param summary generated plan summary
 * @param probableCause generated probable cause
 * @param recommendedSteps ordered recommended steps
 * @param correctiveActionDraft generated corrective action draft
 * @param resolutionNotesDraft generated resolution notes draft
 * @param escalationRecommended whether escalation is recommended
 * @param escalationUrgency recommended escalation urgency
 * @param escalationReason reason for the escalation recommendation
 * @param requiredEvidence evidence requested before approval
 * @param uncertaintyNotes model uncertainty and missing-context notes
 * @param modelProvider AI provider name used for generation
 * @param modelName AI model name used for generation
 * @param providerMetadata non-secret provider metadata
 * @param generatedAt generation timestamp
 * @param approvedAt approval timestamp
 * @param approvedBy approval actor
 * @param rejectedAt rejection timestamp
 * @param rejectedBy rejection actor
 * @param rejectionReason rejection reason
 * @param finalCorrectiveAction final operator-approved corrective action
 * @param finalResolutionNotes final operator-approved resolution notes
 * @since 1.0
 */
public record AiResolutionPlanResource(
        Long id,
        Long organizationId,
        Long incidentId,
        String status,
        String summary,
        String probableCause,
        List<AiResolutionPlanStepResource> recommendedSteps,
        String correctiveActionDraft,
        String resolutionNotesDraft,
        Boolean escalationRecommended,
        String escalationUrgency,
        String escalationReason,
        List<String> requiredEvidence,
        List<String> uncertaintyNotes,
        String modelProvider,
        String modelName,
        String providerMetadata,
        Instant generatedAt,
        Instant approvedAt,
        String approvedBy,
        Instant rejectedAt,
        String rejectedBy,
        String rejectionReason,
        String finalCorrectiveAction,
        String finalResolutionNotes
) {
}
