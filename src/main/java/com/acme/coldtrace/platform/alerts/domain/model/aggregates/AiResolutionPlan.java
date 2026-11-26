package com.acme.coldtrace.platform.alerts.domain.model.aggregates;

import com.acme.coldtrace.platform.alerts.domain.model.commands.ApproveAiResolutionPlanCommand;
import com.acme.coldtrace.platform.alerts.domain.model.commands.CreateAiResolutionPlanCommand;
import com.acme.coldtrace.platform.alerts.domain.model.commands.RejectAiResolutionPlanCommand;
import com.acme.coldtrace.platform.alerts.domain.model.valueobjects.AiResolutionPlanStatus;
import com.acme.coldtrace.platform.alerts.domain.model.valueobjects.AiResolutionPlanStep;
import com.acme.coldtrace.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * AI resolution plan audit record for an incident.
 * <p>
 * The record stores generated advisory content, provider metadata, and the
 * human decision lifecycle without storing secrets or raw hidden prompts.
 *
 * @since 1.0
 */
@Getter
public class AiResolutionPlan extends AbstractDomainAggregateRoot<AiResolutionPlan> {
    private Long id;
    private Long organizationId;
    private Long incidentId;
    private AiResolutionPlanStatus status;
    private String summary;
    private String probableCause;
    private List<AiResolutionPlanStep> recommendedSteps;
    private String correctiveActionDraft;
    private String resolutionNotesDraft;
    private Boolean escalationRecommended;
    private String escalationUrgency;
    private String escalationReason;
    private List<String> requiredEvidence;
    private List<String> uncertaintyNotes;
    private String modelProvider;
    private String modelName;
    private String providerMetadata;
    private Instant generatedAt;
    private Instant approvedAt;
    private String approvedBy;
    private Instant rejectedAt;
    private String rejectedBy;
    private String rejectionReason;
    private String finalCorrectiveAction;
    private String finalResolutionNotes;

    protected AiResolutionPlan() {
    }

    /**
     * Creates a pending AI resolution plan from generated content.
     *
     * @param command command containing generated plan content and metadata
     */
    public AiResolutionPlan(CreateAiResolutionPlanCommand command) {
        this.organizationId = command.organizationId();
        this.incidentId = command.incidentId();
        this.status = AiResolutionPlanStatus.PENDING;
        this.summary = command.summary();
        this.probableCause = command.probableCause();
        this.recommendedSteps = List.copyOf(command.recommendedSteps());
        this.correctiveActionDraft = command.correctiveActionDraft();
        this.resolutionNotesDraft = command.resolutionNotesDraft();
        this.escalationRecommended = command.escalationRecommended();
        this.escalationUrgency = command.escalationUrgency();
        this.escalationReason = command.escalationReason();
        this.requiredEvidence = List.copyOf(command.requiredEvidence());
        this.uncertaintyNotes = List.copyOf(command.uncertaintyNotes());
        this.modelProvider = command.modelProvider();
        this.modelName = command.modelName();
        this.providerMetadata = command.providerMetadata();
        this.generatedAt = Instant.now();
    }

    /**
     * Rebuilds an AI resolution plan from persisted state.
     */
    public AiResolutionPlan(
            Long id,
            Long organizationId,
            Long incidentId,
            AiResolutionPlanStatus status,
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
        this.id = id;
        this.organizationId = organizationId;
        this.incidentId = incidentId;
        this.status = status;
        this.summary = summary;
        this.probableCause = probableCause;
        this.recommendedSteps = List.copyOf(recommendedSteps);
        this.correctiveActionDraft = correctiveActionDraft;
        this.resolutionNotesDraft = resolutionNotesDraft;
        this.escalationRecommended = escalationRecommended;
        this.escalationUrgency = escalationUrgency;
        this.escalationReason = escalationReason;
        this.requiredEvidence = List.copyOf(requiredEvidence);
        this.uncertaintyNotes = List.copyOf(uncertaintyNotes);
        this.modelProvider = modelProvider;
        this.modelName = modelName;
        this.providerMetadata = providerMetadata;
        this.generatedAt = generatedAt;
        this.approvedAt = approvedAt;
        this.approvedBy = approvedBy;
        this.rejectedAt = rejectedAt;
        this.rejectedBy = rejectedBy;
        this.rejectionReason = rejectionReason;
        this.finalCorrectiveAction = finalCorrectiveAction;
        this.finalResolutionNotes = finalResolutionNotes;
    }

    /**
     * Marks this plan as approved and stores the final operator-edited values.
     *
     * @param command approval command
     */
    public void approve(ApproveAiResolutionPlanCommand command) {
        this.status = AiResolutionPlanStatus.APPROVED;
        this.approvedAt = Instant.now();
        this.approvedBy = command.approvedBy();
        this.finalCorrectiveAction = command.finalCorrectiveAction();
        this.finalResolutionNotes = command.finalResolutionNotes();
    }

    /**
     * Marks this plan as rejected and stores audit metadata.
     *
     * @param command rejection command
     */
    public void reject(RejectAiResolutionPlanCommand command) {
        this.status = AiResolutionPlanStatus.REJECTED;
        this.rejectedAt = Instant.now();
        this.rejectedBy = command.rejectedBy();
        this.rejectionReason = command.rejectionReason();
    }

    /** @return true when the generated plan still awaits a human decision */
    public boolean isPending() {
        return AiResolutionPlanStatus.PENDING.equals(this.status);
    }
}
