package com.acme.coldtrace.platform.alerts.infrastructure.ai;

import com.acme.coldtrace.platform.alerts.application.ports.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Reads only Alert-owned rows for the existing incident-plan use cases. */
@Component
public class LocalIncidentEvidenceReader implements IncidentEvidenceReader {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  public LocalIncidentEvidenceReader(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }
  private static Long number(Object value) { return value == null ? null : ((Number)value).longValue(); }
  private static Instant time(Object value) {
    if (value == null) return null;
    if (value instanceof Timestamp timestamp) return timestamp.toInstant();
    if (value instanceof java.time.LocalDateTime dateTime) return dateTime.toInstant(java.time.ZoneOffset.UTC);
    throw new IllegalArgumentException("Unsupported SQL timestamp");
  }
  private static String kind(String type,JsonNode context) {
    if (!"THRESHOLD_BREACHED".equals(type)) return type;
    if (outside(context,"temperature","safeMinTemperature","safeMaxTemperature")) return "temperature";
    if (outside(context,"humidity","safeMinHumidity","safeMaxHumidity")) return "humidity";
    return "THRESHOLD_BREACHED";
  }
  private static boolean outside(JsonNode context,String value,String min,String max) {
    return context.path(value).isNumber() && context.path(min).isNumber() && context.path(max).isNumber()
        && (context.get(value).asDouble()<context.get(min).asDouble() || context.get(value).asDouble()>context.get(max).asDouble());
  }
  public boolean existsByIdAndOrganizationId(Long id, Long org) {
    return Boolean.TRUE.equals(jdbc.queryForObject("SELECT COUNT(*)>0 FROM incidents WHERE id=? AND organization_id=?", Boolean.class, id, org));
  }
  public Optional<IncidentEvidenceSnapshot> findByIdAndOrganizationId(Long id, Long org) {
    var rows = jdbc.queryForList("SELECT * FROM incidents WHERE id=? AND organization_id=?", id, org);
    if (rows.isEmpty()) return Optional.empty();
    var row = rows.getFirst();
    JsonNode context = json.createObjectNode();
    if (row.get("context_payload") != null) {
      Object raw = row.get("context_payload");
      try { context = json.readTree(raw instanceof byte[] bytes ? new String(bytes, java.nio.charset.StandardCharsets.UTF_8) : raw.toString()); }
      catch (Exception invalid) { throw new IllegalStateException("Invalid stored incident context", invalid); }
    }
    var actions = jdbc.queryForList("SELECT description,created_at FROM corrective_actions WHERE incident_id=? AND organization_id=? ORDER BY id DESC LIMIT 1", id, org);
    Map<String,Object> action = actions.isEmpty() ? Map.of() : actions.getFirst();
    return Optional.of(new IncidentEvidenceSnapshot(number(row.get("id")), org,
        number(row.get("asset_id")), number(row.get("device_id")), number(row.get("reading_id")),
        context.path("assetName").asText(null), context.path("deviceName").asText(null),
        kind((String)row.get("type"),context), (String)row.get("severity"), (String)row.get("status"),
        context.path("value").asText(null), time(row.get("detected_at")), time(row.get("acknowledged_at")),
        null, null, null, null, time(action.get("created_at")), null, (String)action.get("description")));
  }
}
