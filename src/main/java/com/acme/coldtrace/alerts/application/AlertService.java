package com.acme.coldtrace.alerts.application;

import com.acme.coldtrace.alerts.domain.AcknowledgementPolicy;
import com.acme.coldtrace.alerts.infrastructure.IdentityGateway.UserRef;
import com.acme.coldtrace.shared.IdentitySession;
import com.fasterxml.jackson.databind.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AlertService {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final Clock clock;
  private final AlertUpdates updates;

  public AlertService(JdbcTemplate jdbc, ObjectMapper json, Clock clock, AlertUpdates updates) {
    this.jdbc = jdbc;
    this.json = json;
    this.clock = clock;
    this.updates = updates;
  }

  private Map<String, Object> incident(long id, long org, boolean lock) {
    var rows =
        jdbc.queryForList(
            "SELECT * FROM incidents WHERE id=? AND organization_id=?"
                + (lock ? " FOR UPDATE" : ""),
            id,
            org);
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return rows.get(0);
  }

  private static Instant instant(Object value) {
    if (value == null) return null;
    if (value instanceof Timestamp ts) return ts.toInstant();
    if (value instanceof LocalDateTime local) return local.toInstant(ZoneOffset.UTC);
    return Instant.parse(value.toString());
  }

  public List<Map<String, Object>> list(long org) {
    return jdbc.queryForList(
        "SELECT id,asset_id,type,severity,status,detected_at,assigned_to_user_id,assignee_name FROM"
            + " incidents WHERE organization_id=? ORDER BY id DESC LIMIT 100",
        org);
  }

  public List<Map<String, Object>> notifications(long org) {
    return jdbc.queryForList(
        "SELECT id,incident_id,channel,status,deep_link,created_at FROM notifications WHERE"
            + " organization_id=? ORDER BY id DESC LIMIT 100",
        org);
  }

  public List<Detail> traceability(
      IdentitySession session, List<UserRef> members, Instant from, Instant to) {
    if (from == null
        || to == null
        || !from.isBefore(to)
        || Duration.between(from, to).toDays() > 90)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report period");
    return jdbc
        .queryForList(
            "SELECT id FROM incidents WHERE organization_id=? AND detected_at>=? AND detected_at<?"
                + " ORDER BY detected_at,id",
            Long.class,
            session.organizationId(),
            Timestamp.from(from),
            Timestamp.from(to))
        .stream()
        .map(id -> detail(id, session, members))
        .toList();
  }

  public record AssetRef(long assetId, String name, String locationName) {}

  public record ReadingRef(
      Long readingId, Double temperature, Double humidity, String recordedAt) {}

  public record RangeRef(
      Double minTemperature, Double maxTemperature, Double minHumidity, Double maxHumidity) {}

  public record Detail(
      long id,
      String type,
      String severity,
      String state,
      Instant detectedAt,
      AssetRef asset,
      ReadingRef reading,
      RangeRef safeRange,
      UserRef assignee,
      UserRef suggestedAssignee,
      List<String> allowedActions,
      List<Map<String, Object>> correctiveActions) {}

  public record Result(
      long id,
      String state,
      UserRef assignee,
      Instant acknowledgedAt,
      Instant undoAvailableUntil) {}

  private JsonNode context(Map<String, Object> row) {
    var raw = row.get("context_payload");
    if (raw == null) return json.createObjectNode();
    try {
      var node =
          json.readTree(
              raw instanceof byte[] bytes
                  ? new String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                  : raw.toString());
      return node.isTextual() ? json.readTree(node.asText()) : node;
    } catch (Exception e) {
      throw new IllegalStateException("Invalid persisted incident context", e);
    }
  }

  private Double number(JsonNode node, String key) {
    return node.hasNonNull(key) ? node.get(key).doubleValue() : null;
  }

  private UserRef assignee(Map<String, Object> row) {
    return row.get("assigned_to_user_id") == null
        ? null
        : new UserRef(
            ((Number) row.get("assigned_to_user_id")).longValue(),
            (String) row.get("assignee_name"));
  }

  public Detail detail(long id, IdentitySession session, List<UserRef> members) {
    var row = incident(id, session.organizationId(), false);
    var context = context(row);
    long asset = ((Number) row.get("asset_id")).longValue();
    var recent =
        jdbc.queryForList(
            "SELECT assigned_to_user_id FROM incidents WHERE organization_id=? AND asset_id=? AND"
                + " acknowledged_at>=? AND assigned_to_user_id IS NOT NULL ORDER BY acknowledged_at"
                + " DESC,id DESC LIMIT 1",
            session.organizationId(),
            asset,
            Timestamp.from(clock.instant().minus(Duration.ofDays(30))));
    Long suggested =
        recent.isEmpty()
            ? session.userId()
            : ((Number) recent.get(0).get("assigned_to_user_id")).longValue();
    var suggestion =
        members.stream()
            .filter(member -> member.userId().equals(suggested))
            .findFirst()
            .orElse(new UserRef(session.userId(), session.fullName()));
    var actions = new ArrayList<String>();
    String state = (String) row.get("status");
    if (session.permits("alerts:update")) {
      if (state.equals("OPEN")) actions.add("ACKNOWLEDGE_AND_ASSIGN");
      if (AcknowledgementPolicy.undoAvailable(
              state, instant(row.get("acknowledged_at")), clock.instant())
          && Objects.equals(row.get("acknowledged_by_user_id"), session.userId()))
        actions.add("UNDO_ACKNOWLEDGEMENT");
      if (!state.equals("RESOLVED")) {
        actions.add("REGISTER_CORRECTIVE_ACTION");
        actions.add("RESOLVE");
      }
    }
    return new Detail(
        id,
        (String) row.get("type"),
        (String) row.get("severity"),
        state,
        instant(row.get("detected_at")),
        new AssetRef(
            asset,
            context.path("assetName").asText("Asset " + asset),
            context.path("locationName").asText(null)),
        row.get("reading_id") == null
            ? null
            : new ReadingRef(
                ((Number) row.get("reading_id")).longValue(),
                number(context, "temperature"),
                number(context, "humidity"),
                context.path("recordedAt").asText(null)),
        context.has("safeMinTemperature")
            ? new RangeRef(
                number(context, "safeMinTemperature"),
                number(context, "safeMaxTemperature"),
                number(context, "safeMinHumidity"),
                number(context, "safeMaxHumidity"))
            : null,
        assignee(row),
        suggestion,
        actions,
        jdbc.queryForList(
            "SELECT id,author_user_id,description,created_at FROM corrective_actions WHERE"
                + " incident_id=? AND organization_id=? ORDER BY id",
            id,
            session.organizationId()));
  }

  public static void authorize(IdentitySession session) {
    if (!session.permits("alerts:update"))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ALERT_UPDATE_FORBIDDEN");
  }

  @Transactional
  public Result acknowledge(long id, IdentitySession session, UserRef assignee) {
    authorize(session);
    var row = incident(id, session.organizationId(), true);
    if (!"OPEN".equals(row.get("status")))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "ALERT_NOT_OPEN");
    var now = clock.instant();
    jdbc.update(
        "UPDATE incidents SET"
            + " status='ACKNOWLEDGED',assigned_to_user_id=?,assignee_name=?,acknowledged_by_user_id=?,acknowledged_at=?,assigned_at=?"
            + " WHERE id=?",
        assignee.userId(),
        assignee.fullName(),
        session.userId(),
        Timestamp.from(now),
        Timestamp.from(now),
        id);
    updates.append(
        id,
        session.organizationId(),
        ((Number) row.get("asset_id")).longValue(),
        "ACKNOWLEDGED",
        assignee.userId(),
        now);
    return new Result(id, "ACKNOWLEDGED", assignee, now, now.plusSeconds(10));
  }

  @Transactional
  public Result undo(long id, IdentitySession session) {
    authorize(session);
    var row = incident(id, session.organizationId(), true);
    var now = clock.instant();
    if (!"ACKNOWLEDGED".equals(row.get("status")))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "ALERT_NOT_ACKNOWLEDGED");
    if (!Objects.equals(row.get("acknowledged_by_user_id"), session.userId()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ACKNOWLEDGEMENT_AUTHOR_REQUIRED");
    if (!AcknowledgementPolicy.undoAvailable(
        "ACKNOWLEDGED", instant(row.get("acknowledged_at")), now))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "UNDO_WINDOW_EXPIRED");
    jdbc.update(
        "UPDATE incidents SET"
            + " status='OPEN',assigned_to_user_id=NULL,assignee_name=NULL,acknowledged_by_user_id=NULL,acknowledged_at=NULL,assigned_at=NULL"
            + " WHERE id=?",
        id);
    updates.append(
        id,
        session.organizationId(),
        ((Number) row.get("asset_id")).longValue(),
        "OPEN",
        null,
        now);
    return new Result(id, "OPEN", null, null, null);
  }

  @Transactional
  public void action(long id, IdentitySession session, String description) {
    authorize(session);
    var row = incident(id, session.organizationId(), true);
    if ("RESOLVED".equals(row.get("status")))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "ALERT_ALREADY_RESOLVED");
    if (description == null || description.isBlank() || description.length() > 2000)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid corrective action");
    jdbc.update(
        "INSERT INTO"
            + " corrective_actions(incident_id,organization_id,author_user_id,description,created_at)"
            + " VALUES (?,?,?,?,?)",
        id,
        session.organizationId(),
        session.userId(),
        description.trim(),
        Timestamp.from(clock.instant()));
  }

  @Transactional
  public Result resolve(long id, IdentitySession session, String summary) {
    authorize(session);
    var row = incident(id, session.organizationId(), true);
    if ("RESOLVED".equals(row.get("status")))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "ALERT_ALREADY_RESOLVED");
    if (summary == null || summary.isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resolution summary required");
    action(id, session, summary);
    var now = clock.instant();
    jdbc.update(
        "UPDATE incidents SET status='RESOLVED',resolved_at=? WHERE id=?", Timestamp.from(now), id);
    updates.append(
        id,
        session.organizationId(),
        ((Number) row.get("asset_id")).longValue(),
        "RESOLVED",
        row.get("assigned_to_user_id") == null
            ? null
            : ((Number) row.get("assigned_to_user_id")).longValue(),
        now);
    return new Result(id, "RESOLVED", assignee(row), instant(row.get("acknowledged_at")), null);
  }
}
