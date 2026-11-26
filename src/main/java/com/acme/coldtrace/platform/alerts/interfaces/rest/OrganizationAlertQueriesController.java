package com.acme.coldtrace.platform.alerts.interfaces.rest;

import com.acme.coldtrace.alerts.infrastructure.IdentityGateway;
import com.acme.coldtrace.platform.alerts.application.ports.IncidentEvidenceReader;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.*;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Preserves the organization-scoped read contracts consumed by the dashboard. */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
public class OrganizationAlertQueriesController {
  private final IdentityGateway identity;
  private final IncidentEvidenceReader incidents;
  private final JdbcTemplate jdbc;
  public OrganizationAlertQueriesController(IdentityGateway identity,IncidentEvidenceReader incidents,JdbcTemplate jdbc) {
    this.identity=identity;this.incidents=incidents;this.jdbc=jdbc;
  }
  private void authorize(String bearer,long org) {
    if(!identity.session(bearer).organizationId().equals(org))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }
  private static Instant time(Object value) {
    if(value==null)return null;
    if(value instanceof Timestamp ts)return ts.toInstant();
    if(value instanceof LocalDateTime dt)return dt.toInstant(ZoneOffset.UTC);
    throw new IllegalArgumentException("Unsupported SQL timestamp");
  }
  private IncidentResource resource(long org,long id) {
    var incident=incidents.findByIdAndOrganizationId(id,org).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    var notices=jdbc.queryForList("SELECT status,created_at FROM notifications WHERE incident_id=? AND organization_id=? ORDER BY id DESC",id,org);
    var resolved=jdbc.queryForObject("SELECT resolved_at FROM incidents WHERE id=? AND organization_id=?",(row,index)->row.getObject(1),id,org);
    String type="NO_DATA".equals(incident.type())?"connectivity":
        "THRESHOLD_BREACHED".equals(incident.type())?"other":incident.type().toLowerCase(Locale.ROOT);
    return new IncidentResource(id,org,incident.assetId(),incident.deviceId(),incident.readingId(),
        incident.assetName(),incident.deviceName(),type,incident.severity().toLowerCase(Locale.ROOT),
        incident.status().toLowerCase(Locale.ROOT),incident.value(),incident.detectedAt(),incident.acknowledgedAt(),
        incident.acknowledgedBy(),incident.escalatedAt(),incident.escalatedBy(),incident.escalationReason(),
        incident.correctiveActionRegisteredAt(),incident.correctiveActionRegisteredBy(),incident.correctiveAction(),
        time(resolved),null,null,notices.isEmpty()?null:availableStatus((String)notices.getFirst().get("status")),
        notices.isEmpty()?null:time(notices.getFirst().get("created_at")),notices.size());
  }
  private static String availableStatus(String status) {
    return "AVAILABLE".equals(status)?"sent":status.toLowerCase(Locale.ROOT);
  }
  @GetMapping("/incidents")
  public List<IncidentResource> list(@PathVariable long organizationId,
      @RequestHeader(value="Authorization",required=false)String bearer) {
    authorize(bearer,organizationId);
    return jdbc.queryForList("SELECT id FROM incidents WHERE organization_id=? ORDER BY id DESC LIMIT 100",Long.class,organizationId)
        .stream().map(id->resource(organizationId,id)).toList();
  }
  @GetMapping("/incidents/{incidentId}")
  public IncidentResource detail(@PathVariable long organizationId,@PathVariable long incidentId,
      @RequestHeader(value="Authorization",required=false)String bearer) {
    authorize(bearer,organizationId);return resource(organizationId,incidentId);
  }
  private List<NotificationResource> notices(long org,Long incident) {
    String where=" WHERE organization_id=?"+(incident==null?"":" AND incident_id=?");
    Object[] values=incident==null?new Object[]{org}:new Object[]{org,incident};
    return jdbc.queryForList("SELECT * FROM notifications"+where+" ORDER BY id DESC LIMIT 100",values).stream()
        .map(row->new NotificationResource(((Number)row.get("id")).longValue(),org,
            ((Number)row.get("incident_id")).longValue(),"IN_APP".equals(row.get("channel"))?"app":row.get("channel").toString().toLowerCase(Locale.ROOT),
            "organization:"+org,(String)row.get("deep_link"),availableStatus((String)row.get("status")),
            time(row.get("created_at")),null,null)).toList();
  }
  @GetMapping("/notifications")
  public List<NotificationResource> notifications(@PathVariable long organizationId,
      @RequestHeader(value="Authorization",required=false)String bearer) {
    authorize(bearer,organizationId);return notices(organizationId,null);
  }
  @GetMapping("/incidents/{incidentId}/notifications")
  public List<NotificationResource> incidentNotifications(@PathVariable long organizationId,@PathVariable long incidentId,
      @RequestHeader(value="Authorization",required=false)String bearer) {
    authorize(bearer,organizationId);
    if(!incidents.existsByIdAndOrganizationId(incidentId,organizationId))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return notices(organizationId,incidentId);
  }
}
