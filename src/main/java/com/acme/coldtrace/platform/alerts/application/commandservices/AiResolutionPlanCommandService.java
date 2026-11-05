package com.acme.coldtrace.platform.alerts.application.commandservices;

import com.acme.coldtrace.platform.alerts.domain.model.aggregates.AiResolutionPlan;
import com.acme.coldtrace.platform.alerts.domain.model.commands.ApproveAiResolutionPlanCommand;
import com.acme.coldtrace.platform.alerts.domain.model.commands.CreateAiResolutionPlanCommand;
import com.acme.coldtrace.platform.alerts.domain.model.commands.GenerateAiResolutionPlanCommand;
import com.acme.coldtrace.platform.alerts.domain.model.commands.RejectAiResolutionPlanCommand;
import com.acme.coldtrace.platform.shared.application.result.Result;

/**
 * Application service contract for AI resolution plan persistence commands.
 *
 * @since 1.0
 */
public interface AiResolutionPlanCommandService {
    Result<AiResolutionPlan, AiResolutionPlanCommandFailure> handle(GenerateAiResolutionPlanCommand command);

    Result<AiResolutionPlan, AiResolutionPlanCommandFailure> handle(CreateAiResolutionPlanCommand command);

    Result<AiResolutionPlan, AiResolutionPlanCommandFailure> handle(ApproveAiResolutionPlanCommand command);

    Result<AiResolutionPlan, AiResolutionPlanCommandFailure> handle(RejectAiResolutionPlanCommand command);
}
