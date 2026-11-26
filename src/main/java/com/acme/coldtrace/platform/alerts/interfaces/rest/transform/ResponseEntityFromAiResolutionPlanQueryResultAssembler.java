package com.acme.coldtrace.platform.alerts.interfaces.rest.transform;

import com.acme.coldtrace.platform.alerts.application.queryservices.AiResolutionPlanQueryFailure;
import com.acme.coldtrace.platform.alerts.domain.model.aggregates.AiResolutionPlan;
import com.acme.coldtrace.platform.shared.application.result.Result;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Interface layer translator converting AI resolution plan query results to HTTP responses.
 *
 * @since 1.0
 */
public class ResponseEntityFromAiResolutionPlanQueryResultAssembler {
    /**
     * Converts a plan history query result into an HTTP response.
     *
     * @param result AI resolution plan list query result
     * @param messageSource message source for localized failure details
     * @return 200 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromListResult(
            Result<List<AiResolutionPlan>, AiResolutionPlanQueryFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                plans -> ResponseEntity.ok(plans.stream()
                        .map(AiResolutionPlanResourceFromEntityAssembler::toResourceFromEntity)
                        .toList()),
                failure -> toFailureResponse(failure, messageSource)
        );
    }

    private static ResponseEntity<?> toFailureResponse(
            AiResolutionPlanQueryFailure failure,
            MessageSource messageSource
    ) {
        var status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(
                status,
                messageSource.getMessage(
                        failure.messageKey(),
                        failure.args(),
                        failure.messageKey(),
                        LocaleContextHolder.getLocale()
                )
        ));
    }
}
