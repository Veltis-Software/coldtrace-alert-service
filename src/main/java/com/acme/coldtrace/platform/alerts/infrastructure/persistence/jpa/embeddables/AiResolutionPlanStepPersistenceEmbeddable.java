package com.acme.coldtrace.platform.alerts.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistence embeddable for one AI resolution plan step.
 *
 * @since 1.0
 */
@Getter
@Setter
@Embeddable
public class AiResolutionPlanStepPersistenceEmbeddable {
    @Column(name = "step_sequence", nullable = false)
    private Integer sequence;

    @Column(nullable = false, length = 400)
    private String action;

    @Column(nullable = false, length = 500)
    private String rationale;

    @Column(nullable = false, length = 400)
    private String expectedOutcome;

    protected AiResolutionPlanStepPersistenceEmbeddable() {
    }

    /**
     * Creates a persistence step value.
     *
     * @param sequence execution order
     * @param action operator-facing action
     * @param rationale reason for the action
     * @param expectedOutcome expected operational result
     */
    public AiResolutionPlanStepPersistenceEmbeddable(
            Integer sequence,
            String action,
            String rationale,
            String expectedOutcome
    ) {
        this.sequence = sequence;
        this.action = action;
        this.rationale = rationale;
        this.expectedOutcome = expectedOutcome;
    }
}
