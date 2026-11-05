package com.acme.coldtrace.platform.alerts.interfaces.rest;

import com.acme.coldtrace.alerts.application.AlertService;
import com.acme.coldtrace.alerts.infrastructure.IdentityGateway;
import com.acme.coldtrace.shared.IdentitySession;
import com.acme.coldtrace.platform.alerts.application.commandservices.AiResolutionPlanCommandService;
import com.acme.coldtrace.platform.alerts.application.queryservices.AiResolutionPlanQueryService;
import com.acme.coldtrace.platform.alerts.domain.model.commands.*;
import com.acme.coldtrace.platform.alerts.domain.model.queries.GetAiResolutionPlansByIncidentIdAndOrganizationIdQuery;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.*;
import com.acme.coldtrace.platform.alerts.interfaces.rest.transform.*;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Preserves the original incident-plan API while keeping decisions in Alert. */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/incidents/{incidentId}/ai-resolution-plans")
public class IncidentAiPlansController {
  private final IdentityGateway identity;
  private final AiResolutionPlanCommandService commands;
  private final AiResolutionPlanQueryService queries;
  private final MessageSource messages;
  public IncidentAiPlansController(IdentityGateway identity, AiResolutionPlanCommandService commands,
      AiResolutionPlanQueryService queries, MessageSource messages) {
    this.identity = identity; this.commands = commands; this.queries = queries; this.messages = messages;
  }
  private IdentitySession authorize(String bearer, long org, boolean decision) {
    var session = identity.session(bearer);
    if (!session.organizationId().equals(org)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    if (decision) AlertService.authorize(session);
    return session;
  }
  @PostMapping
  public ResponseEntity<?> generate(@PathVariable Long organizationId, @PathVariable Long incidentId,
      @RequestHeader(value="Authorization",required=false) String bearer) {
    authorize(bearer, organizationId, false);
    return ResponseEntityFromAiResolutionPlanCommandResultAssembler.toResponseEntityFromCreateResult(
        commands.handle(new GenerateAiResolutionPlanCommand(organizationId, incidentId)), messages);
  }
  @GetMapping
  public ResponseEntity<?> history(@PathVariable Long organizationId, @PathVariable Long incidentId,
      @RequestHeader(value="Authorization",required=false) String bearer) {
    authorize(bearer, organizationId, false);
    return ResponseEntityFromAiResolutionPlanQueryResultAssembler.toResponseEntityFromListResult(
        queries.handle(new GetAiResolutionPlansByIncidentIdAndOrganizationIdQuery(organizationId, incidentId)), messages);
  }
  @PostMapping("/{planId}/approvals")
  public ResponseEntity<?> approve(@PathVariable Long organizationId, @PathVariable Long incidentId,
      @PathVariable Long planId, @Valid @RequestBody ApproveAiResolutionPlanResource body,
      @RequestHeader(value="Authorization",required=false) String bearer) {
    var session = authorize(bearer, organizationId, true);
    return ResponseEntityFromAiResolutionPlanCommandResultAssembler.toResponseEntityFromLifecycleResult(
        commands.handle(new ApproveAiResolutionPlanCommand(organizationId, incidentId, planId,
            session.fullName(), body.finalCorrectiveAction(), body.finalResolutionNotes())), messages);
  }
  @PostMapping("/{planId}/rejections")
  public ResponseEntity<?> reject(@PathVariable Long organizationId, @PathVariable Long incidentId,
      @PathVariable Long planId, @Valid @RequestBody RejectAiResolutionPlanResource body,
      @RequestHeader(value="Authorization",required=false) String bearer) {
    var session = authorize(bearer, organizationId, true);
    return ResponseEntityFromAiResolutionPlanCommandResultAssembler.toResponseEntityFromLifecycleResult(
        commands.handle(new RejectAiResolutionPlanCommand(organizationId, incidentId, planId,
            session.fullName(), body.rejectionReason())), messages);
  }
}
