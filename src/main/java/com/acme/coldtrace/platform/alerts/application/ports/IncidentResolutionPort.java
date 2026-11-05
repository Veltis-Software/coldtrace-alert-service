package com.acme.coldtrace.platform.alerts.application.ports;
import com.acme.coldtrace.platform.alerts.application.commandservices.AiResolutionPlanCommandFailure;
import com.acme.coldtrace.platform.alerts.domain.model.commands.ResolveIncidentWithCorrectiveActionCommand;
import com.acme.coldtrace.platform.shared.application.result.Result;
public interface IncidentResolutionPort {
 Result<Boolean,AiResolutionPlanCommandFailure> handle(ResolveIncidentWithCorrectiveActionCommand command);
}
