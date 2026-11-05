package com.acme.coldtrace.platform.alerts.domain.model.valueobjects;

import lombok.Getter;

/**
 * One ordered advisory action stored with an AI resolution plan.
 *
 * @since 1.0
 */
@Getter
public class AiResolutionPlanStep {
    private final Integer sequence;
    private final String action;
    private final String rationale;
    private final String expectedOutcome;

    /**
     * Creates an advisory plan step.
     *
     * @param sequence execution order
     * @param action operator-facing action
     * @param rationale reason for the action
     * @param expectedOutcome expected operational result
     */
    public AiResolutionPlanStep(
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
