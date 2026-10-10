package com.acme.coldtrace.alerts;

import com.acme.coldtrace.alerts.application.AlertService;
import com.acme.coldtrace.alerts.infrastructure.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class AlertController {
  private final IdentityGateway identity;
  private final AlertService service;
  private final AlertStream stream;

  public AlertController(IdentityGateway identity, AlertService service, AlertStream stream) {
    this.identity = identity;
    this.service = service;
    this.stream = stream;
  }

  @GetMapping({"/api/v1/alerts", "/api/v1/incidents"})
  public List<Map<String, Object>> list(
      @RequestHeader(value = "Authorization", required = false) String bearer) {
    return service.list(identity.session(bearer).organizationId());
  }

  @GetMapping("/api/v1/notifications")
  public List<Map<String, Object>> notifications(
      @RequestHeader(value = "Authorization", required = false) String bearer) {
    return service.notifications(identity.session(bearer).organizationId());
  }

  @GetMapping({"/api/v1/alerts/{id}", "/api/v1/alerts/{id}/detail", "/api/v1/incidents/{id}"})
  public AlertService.Detail detail(
      @PathVariable long id,
      @RequestHeader(value = "Authorization", required = false) String bearer) {
    var session = identity.session(bearer);
    return service.detail(id, session, identity.members(bearer, session));
  }

  @GetMapping("/api/v1/alerts/assignees")
  public List<IdentityGateway.UserRef> assignees(
      @RequestHeader(value = "Authorization", required = false) String bearer) {
    var session = identity.session(bearer);
    return identity.members(bearer, session);
  }

  public record Assignment(Long assigneeUserId) {}

  @GetMapping("/api/v1/incidents/traceability")
  public List<AlertService.Detail> traceability(
      @RequestParam java.time.Instant from,
      @RequestParam java.time.Instant to,
      @RequestHeader(value = "Authorization", required = false) String bearer) {
    var session = identity.session(bearer);
    return service.traceability(session, identity.members(bearer, session), from, to);
  }

  @PostMapping("/api/v1/alerts/{id}/acknowledgement")
  public AlertService.Result acknowledge(
      @PathVariable long id,
      @RequestBody Assignment body,
      @RequestHeader(value = "Authorization", required = false) String bearer) {
    var session = identity.session(bearer);
    AlertService.authorize(session);
    var assignee =
        identity.members(bearer, session).stream()
            .filter(member -> member.userId().equals(body.assigneeUserId()))
            .findFirst()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "ASSIGNEE_NOT_IN_ORGANIZATION"));
    return service.acknowledge(id, session, assignee);
  }

  @DeleteMapping("/api/v1/alerts/{id}/acknowledgement")
  public AlertService.Result undo(
      @PathVariable long id,
      @RequestHeader(value = "Authorization", required = false) String bearer) {
    return service.undo(id, identity.session(bearer));
  }

  @GetMapping(value = "/api/v1/alerts/stream", produces = "text/event-stream")
  public SseEmitter stream(
      @RequestHeader(value = "Authorization", required = false) String bearer,
      @RequestHeader(value = "Last-Event-ID", required = false) String cursor) {
    var session = identity.session(bearer);
    return stream.open(session.organizationId(), bearer, cursor);
  }

  public record Action(String description) {}

  @PostMapping("/api/v1/incidents/{id}/corrective-actions")
  @ResponseStatus(HttpStatus.CREATED)
  public void action(
      @PathVariable long id,
      @RequestBody Action action,
      @RequestHeader(value = "Authorization", required = false) String bearer) {
    service.action(id, identity.session(bearer), action.description());
  }

  @PostMapping("/api/v1/incidents/{id}/resolution")
  public AlertService.Result resolve(
      @PathVariable long id,
      @RequestBody Action action,
      @RequestHeader(value = "Authorization", required = false) String bearer) {
    return service.resolve(id, identity.session(bearer), action.description());
  }
}
