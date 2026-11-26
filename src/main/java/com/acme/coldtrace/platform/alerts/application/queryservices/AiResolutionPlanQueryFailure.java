package com.acme.coldtrace.platform.alerts.application.queryservices;

/**
 * Failure types for AI resolution plan query execution.
 *
 * @since 1.0
 */
public sealed interface AiResolutionPlanQueryFailure
        permits AiResolutionPlanQueryFailure.OrganizationNotFound, AiResolutionPlanQueryFailure.IncidentNotFound {
    /** @return message key to resolve through i18n */
    String messageKey();

    /** @return optional message interpolation arguments */
    default Object[] args() {
        return new Object[0];
    }

    /** Organization not found failure. */
    record OrganizationNotFound() implements AiResolutionPlanQueryFailure {
        @Override
        public String messageKey() {
            return "alerts.ai-resolution-plan.error.organization-not-found";
        }
    }

    /** Incident not found or not owned by the organization. */
    record IncidentNotFound() implements AiResolutionPlanQueryFailure {
        @Override
        public String messageKey() {
            return "alerts.ai-resolution-plan.error.incident-not-found";
        }
    }
}
