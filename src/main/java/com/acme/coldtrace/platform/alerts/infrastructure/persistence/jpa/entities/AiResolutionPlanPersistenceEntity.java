package com.acme.coldtrace.platform.alerts.infrastructure.persistence.jpa.entities;

import com.acme.coldtrace.platform.alerts.domain.model.valueobjects.AiResolutionPlanStatus;
import com.acme.coldtrace.platform.alerts.infrastructure.persistence.jpa.embeddables.AiResolutionPlanStepPersistenceEmbeddable;
import com.acme.coldtrace.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA persistence entity for incident AI resolution plan history.
 *
 * @since 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "incident_ai_resolution_plan")
public class AiResolutionPlanPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(nullable = false)
    private Long organizationId;

    @Column(nullable = false)
    private Long incidentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AiResolutionPlanStatus status;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(nullable = false, length = 800)
    private String probableCause;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "incident_ai_resolution_plan_step",
            joinColumns = @JoinColumn(name = "ai_resolution_plan_id")
    )
    @OrderColumn(name = "position")
    private List<AiResolutionPlanStepPersistenceEmbeddable> recommendedSteps = new ArrayList<>();

    @Column(nullable = false, length = 1000)
    private String correctiveActionDraft;

    @Column(nullable = false, length = 1000)
    private String resolutionNotesDraft;

    @Column(nullable = false)
    private Boolean escalationRecommended;

    @Column(nullable = false, length = 120)
    private String escalationUrgency;

    @Column(nullable = false, length = 800)
    private String escalationReason;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "incident_ai_resolution_plan_required_evidence",
            joinColumns = @JoinColumn(name = "ai_resolution_plan_id")
    )
    @OrderColumn(name = "position")
    @Column(name = "required_evidence", nullable = false, length = 400)
    private List<String> requiredEvidence = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "incident_ai_resolution_plan_uncertainty_note",
            joinColumns = @JoinColumn(name = "ai_resolution_plan_id")
    )
    @OrderColumn(name = "position")
    @Column(name = "uncertainty_note", nullable = false, length = 500)
    private List<String> uncertaintyNotes = new ArrayList<>();

    @Column(nullable = false, length = 80)
    private String modelProvider;

    @Column(nullable = false, length = 160)
    private String modelName;

    @Column(length = 1000)
    private String providerMetadata;

    @Column(nullable = false)
    private Instant generatedAt;

    private Instant approvedAt;
    private String approvedBy;
    private Instant rejectedAt;
    private String rejectedBy;

    @Column(length = 1000)
    private String rejectionReason;

    @Column(length = 1000)
    private String finalCorrectiveAction;

    @Column(length = 1000)
    private String finalResolutionNotes;
}
