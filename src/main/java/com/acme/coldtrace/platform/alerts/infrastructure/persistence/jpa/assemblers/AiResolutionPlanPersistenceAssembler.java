package com.acme.coldtrace.platform.alerts.infrastructure.persistence.jpa.assemblers;

import com.acme.coldtrace.platform.alerts.domain.model.aggregates.AiResolutionPlan;
import com.acme.coldtrace.platform.alerts.domain.model.valueobjects.AiResolutionPlanStep;
import com.acme.coldtrace.platform.alerts.infrastructure.persistence.jpa.embeddables.AiResolutionPlanStepPersistenceEmbeddable;
import com.acme.coldtrace.platform.alerts.infrastructure.persistence.jpa.entities.AiResolutionPlanPersistenceEntity;

import java.util.ArrayList;

/**
 * Assembler that translates AI resolution plans between domain and persistence models.
 *
 * @since 1.0
 */
public final class AiResolutionPlanPersistenceAssembler {
    private AiResolutionPlanPersistenceAssembler() {
    }

    /**
     * Converts a persistence entity into a domain aggregate.
     *
     * @param entity persistence entity read from the database
     * @return AI resolution plan aggregate rebuilt from persisted state
     */
    public static AiResolutionPlan toDomainFromPersistence(AiResolutionPlanPersistenceEntity entity) {
        return new AiResolutionPlan(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getIncidentId(),
                entity.getStatus(),
                entity.getSummary(),
                entity.getProbableCause(),
                entity.getRecommendedSteps().stream()
                        .map(AiResolutionPlanPersistenceAssembler::toDomainStep)
                        .toList(),
                entity.getCorrectiveActionDraft(),
                entity.getResolutionNotesDraft(),
                entity.getEscalationRecommended(),
                entity.getEscalationUrgency(),
                entity.getEscalationReason(),
                ListCopy.copyOf(entity.getRequiredEvidence()),
                ListCopy.copyOf(entity.getUncertaintyNotes()),
                entity.getModelProvider(),
                entity.getModelName(),
                entity.getProviderMetadata(),
                entity.getGeneratedAt(),
                entity.getApprovedAt(),
                entity.getApprovedBy(),
                entity.getRejectedAt(),
                entity.getRejectedBy(),
                entity.getRejectionReason(),
                entity.getFinalCorrectiveAction(),
                entity.getFinalResolutionNotes()
        );
    }

    /**
     * Creates a persistence entity from a domain aggregate.
     *
     * @param plan AI resolution plan aggregate
     * @return persistence entity with copied domain state
     */
    public static AiResolutionPlanPersistenceEntity toPersistenceFromDomain(AiResolutionPlan plan) {
        var entity = new AiResolutionPlanPersistenceEntity();
        entity.setId(plan.getId());
        copyDomainState(plan, entity);
        return entity;
    }

    /**
     * Copies mutable domain state into an existing persistence entity.
     *
     * @param plan source domain aggregate
     * @param entity target persistence entity
     */
    public static void copyDomainState(AiResolutionPlan plan, AiResolutionPlanPersistenceEntity entity) {
        entity.setOrganizationId(plan.getOrganizationId());
        entity.setIncidentId(plan.getIncidentId());
        entity.setStatus(plan.getStatus());
        entity.setSummary(plan.getSummary());
        entity.setProbableCause(plan.getProbableCause());
        entity.setRecommendedSteps(plan.getRecommendedSteps().stream()
                .map(AiResolutionPlanPersistenceAssembler::toPersistenceStep)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        entity.setCorrectiveActionDraft(plan.getCorrectiveActionDraft());
        entity.setResolutionNotesDraft(plan.getResolutionNotesDraft());
        entity.setEscalationRecommended(plan.getEscalationRecommended());
        entity.setEscalationUrgency(plan.getEscalationUrgency());
        entity.setEscalationReason(plan.getEscalationReason());
        entity.setRequiredEvidence(new ArrayList<>(plan.getRequiredEvidence()));
        entity.setUncertaintyNotes(new ArrayList<>(plan.getUncertaintyNotes()));
        entity.setModelProvider(plan.getModelProvider());
        entity.setModelName(plan.getModelName());
        entity.setProviderMetadata(plan.getProviderMetadata());
        entity.setGeneratedAt(plan.getGeneratedAt());
        entity.setApprovedAt(plan.getApprovedAt());
        entity.setApprovedBy(plan.getApprovedBy());
        entity.setRejectedAt(plan.getRejectedAt());
        entity.setRejectedBy(plan.getRejectedBy());
        entity.setRejectionReason(plan.getRejectionReason());
        entity.setFinalCorrectiveAction(plan.getFinalCorrectiveAction());
        entity.setFinalResolutionNotes(plan.getFinalResolutionNotes());
    }

    private static AiResolutionPlanStep toDomainStep(AiResolutionPlanStepPersistenceEmbeddable step) {
        return new AiResolutionPlanStep(
                step.getSequence(),
                step.getAction(),
                step.getRationale(),
                step.getExpectedOutcome()
        );
    }

    private static AiResolutionPlanStepPersistenceEmbeddable toPersistenceStep(AiResolutionPlanStep step) {
        return new AiResolutionPlanStepPersistenceEmbeddable(
                step.getSequence(),
                step.getAction(),
                step.getRationale(),
                step.getExpectedOutcome()
        );
    }

    private static final class ListCopy {
        private ListCopy() {
        }

        private static <T> java.util.List<T> copyOf(java.util.List<T> source) {
            return source == null ? java.util.List.of() : java.util.List.copyOf(source);
        }
    }
}
