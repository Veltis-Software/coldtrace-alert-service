package com.acme.coldtrace.platform.alerts.application.commandservices;

import com.acme.coldtrace.platform.aiassistance.application.commandservices.AiAssistanceFailure;
import com.acme.coldtrace.platform.billing.interfaces.acl.PlanEntitlementFailure;
import com.acme.coldtrace.platform.billing.interfaces.acl.SubscriptionBillingContextFacade;

/**
 * Failure types for AI resolution plan command execution.
 *
 * @since 1.0
 */
public sealed interface AiResolutionPlanCommandFailure
        permits AiResolutionPlanCommandFailure.OrganizationNotFound,
        AiResolutionPlanCommandFailure.IncidentNotFound,
        AiResolutionPlanCommandFailure.IncidentNotActive,
        AiResolutionPlanCommandFailure.IncidentAlreadyResolved,
        AiResolutionPlanCommandFailure.PlanNotFound,
        AiResolutionPlanCommandFailure.PlanAlreadyDecided,
        AiResolutionPlanCommandFailure.ProviderFailure,
        AiResolutionPlanCommandFailure.ContextAssemblyFailed,
        AiResolutionPlanCommandFailure.PlanLimitExceeded {
    /** @return message key to resolve through i18n */
    String messageKey();

    /** @return optional message interpolation arguments */
    default Object[] args() {
        return new Object[0];
    }

    /** Organization not found failure. */
    record OrganizationNotFound() implements AiResolutionPlanCommandFailure {
        @Override
        public String messageKey() {
            return "alerts.ai-resolution-plan.error.organization-not-found";
        }
    }

    /** Incident not found failure. */
    record IncidentNotFound() implements AiResolutionPlanCommandFailure {
        @Override
        public String messageKey() {
            return "alerts.ai-resolution-plan.error.incident-not-found";
        }
    }

    /** Incident cannot receive generated plans in its current lifecycle state. */
    record IncidentNotActive() implements AiResolutionPlanCommandFailure {
        @Override
        public String messageKey() {
            return "alerts.ai-resolution-plan.error.incident-not-active";
        }
    }

    /** Incident was resolved before the pending plan could be approved. */
    record IncidentAlreadyResolved() implements AiResolutionPlanCommandFailure {
        @Override
        public String messageKey() {
            return "alerts.ai-resolution-plan.error.incident-already-resolved";
        }
    }

    /** AI resolution plan not found failure. */
    record PlanNotFound() implements AiResolutionPlanCommandFailure {
        @Override
        public String messageKey() {
            return "alerts.ai-resolution-plan.error.plan-not-found";
        }
    }

    /** AI resolution plan has already been approved or rejected. */
    record PlanAlreadyDecided() implements AiResolutionPlanCommandFailure {
        @Override
        public String messageKey() {
            return "alerts.ai-resolution-plan.error.plan-already-decided";
        }
    }

    /** AI provider failure while generating structured plan content. */
    record ProviderFailure(AiAssistanceFailure failure) implements AiResolutionPlanCommandFailure {
        @Override
        public String messageKey() {
            return failure.messageKey();
        }

        @Override
        public Object[] args() {
            return failure.args();
        }
    }

    /** Backend context could not be serialized for the provider prompt. */
    record ContextAssemblyFailed() implements AiResolutionPlanCommandFailure {
        @Override
        public String messageKey() {
            return "alerts.ai-resolution-plan.error.context-unavailable";
        }
    }

    /** Subscription plan does not allow AI guidance. */
    record PlanLimitExceeded(SubscriptionBillingContextFacade.EntitlementCheckSnapshot entitlement)
            implements AiResolutionPlanCommandFailure, PlanEntitlementFailure {
        @Override
        public String messageKey() {
            return "alerts.ai-resolution-plan.error.plan-limit-exceeded";
        }
    }
}
