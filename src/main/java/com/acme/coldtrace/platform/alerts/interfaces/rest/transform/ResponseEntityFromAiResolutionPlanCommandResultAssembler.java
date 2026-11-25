package com.acme.coldtrace.platform.alerts.interfaces.rest.transform;

import com.acme.coldtrace.platform.aiassistance.application.commandservices.AiAssistanceFailure;
import com.acme.coldtrace.platform.alerts.application.commandservices.AiResolutionPlanCommandFailure;
import com.acme.coldtrace.platform.alerts.domain.model.aggregates.AiResolutionPlan;
import com.acme.coldtrace.platform.billing.interfaces.acl.PlanEntitlementFailure;
import com.acme.coldtrace.platform.billing.interfaces.acl.PlanEntitlementProblemProperties;
import com.acme.coldtrace.platform.shared.application.result.Result;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.springframework.http.HttpStatus.CREATED;

/**
 * Interface layer translator converting AI resolution plan command results to HTTP responses.
 *
 * @since 1.0
 */
public final class ResponseEntityFromAiResolutionPlanCommandResultAssembler {
    private ResponseEntityFromAiResolutionPlanCommandResultAssembler() {
    }

    /**
     * Converts an AI resolution plan generation result into an HTTP response.
     *
     * @param result AI resolution plan command result
     * @param messageSource message source for localized failure details
     * @return 201 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromCreateResult(
            Result<AiResolutionPlan, AiResolutionPlanCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                plan -> new ResponseEntity<>(
                        AiResolutionPlanResourceFromEntityAssembler.toResourceFromEntity(plan),
                        CREATED
                ),
                failure -> toFailureResponse(failure, messageSource)
        );
    }

    /**
     * Converts an AI resolution plan lifecycle result into an HTTP response.
     *
     * @param result AI resolution plan command result
     * @param messageSource message source for localized failure details
     * @return 200 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromLifecycleResult(
            Result<AiResolutionPlan, AiResolutionPlanCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                plan -> ResponseEntity.ok(AiResolutionPlanResourceFromEntityAssembler.toResourceFromEntity(plan)),
                failure -> toFailureResponse(failure, messageSource)
        );
    }

    private static ResponseEntity<?> toFailureResponse(
            AiResolutionPlanCommandFailure failure,
            MessageSource messageSource
    ) {
        var status = statusFromFailure(failure);
        var problemDetail = ProblemDetail.forStatusAndDetail(
                status,
                messageSource.getMessage(
                        failure.messageKey(),
                        failure.args(),
                        failure.messageKey(),
                        LocaleContextHolder.getLocale()
                )
        );
        appendPlanEntitlementProperties(problemDetail, failure);
        return ResponseEntity.status(status).body(problemDetail);
    }

    private static HttpStatus statusFromFailure(AiResolutionPlanCommandFailure failure) {
        if (failure instanceof AiResolutionPlanCommandFailure.OrganizationNotFound ||
                failure instanceof AiResolutionPlanCommandFailure.IncidentNotFound ||
                failure instanceof AiResolutionPlanCommandFailure.PlanNotFound) {
            return HttpStatus.NOT_FOUND;
        }
        if (failure instanceof AiResolutionPlanCommandFailure.IncidentNotActive ||
                failure instanceof AiResolutionPlanCommandFailure.IncidentAlreadyResolved ||
                failure instanceof AiResolutionPlanCommandFailure.PlanAlreadyDecided ||
                failure instanceof AiResolutionPlanCommandFailure.PlanLimitExceeded) {
            return HttpStatus.CONFLICT;
        }
        if (failure instanceof AiResolutionPlanCommandFailure.ProviderFailure providerFailure) {
            return statusFromAiFailure(providerFailure.failure());
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static HttpStatus statusFromAiFailure(AiAssistanceFailure failure) {
        if (failure instanceof AiAssistanceFailure.ProviderTimeout) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if (failure instanceof AiAssistanceFailure.InvalidStructuredOutput) {
            return HttpStatus.BAD_GATEWAY;
        }
        return HttpStatus.SERVICE_UNAVAILABLE;
    }

    private static void appendPlanEntitlementProperties(
            ProblemDetail problemDetail,
            AiResolutionPlanCommandFailure failure
    ) {
        if (failure instanceof PlanEntitlementFailure planFailure) {
            PlanEntitlementProblemProperties.from(planFailure.entitlement())
                    .forEach(problemDetail::setProperty);
        }
    }
}
