package com.acme.coldtrace.platform.alerts.infrastructure.ai;

import com.acme.coldtrace.alerts.application.AlertService;
import com.acme.coldtrace.alerts.infrastructure.IdentityGateway;
import com.acme.coldtrace.platform.alerts.application.commandservices.AiResolutionPlanCommandFailure;
import com.acme.coldtrace.platform.alerts.application.ports.IncidentResolutionPort;
import com.acme.coldtrace.platform.alerts.domain.model.commands.ResolveIncidentWithCorrectiveActionCommand;
import com.acme.coldtrace.platform.shared.application.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Runs inside the plan approval transaction, sharing Alert's datasource. */
@Component
public class LocalIncidentResolutionAdapter implements IncidentResolutionPort {
  private final AlertService alerts;
  private final IdentityGateway identity;
  private final HttpServletRequest request;
  public LocalIncidentResolutionAdapter(AlertService alerts, IdentityGateway identity, HttpServletRequest request) {
    this.alerts = alerts; this.identity = identity; this.request = request;
  }
  public Result<Boolean,AiResolutionPlanCommandFailure> handle(ResolveIncidentWithCorrectiveActionCommand command) {
    var session = identity.session(request.getHeader("Authorization"));
    if (!session.organizationId().equals(command.organizationId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    AlertService.authorize(session);
    alerts.action(command.incidentId(), session, command.correctiveAction());
    alerts.resolve(command.incidentId(), session, command.resolutionNotes());
    return Result.success(true);
  }
}
