package com.acme.coldtrace.alerts;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Published owner snapshots for maintenance and advisory AI consumers. */
@RestController
@RequestMapping("/internal/alerts/organizations/{organizationId}/incidents")
public class InternalIncidentController {
  public record IncidentSnapshot(Long id, Long organizationId, Long assetId, String status, Instant detectedAt) {}
  private final JdbcTemplate jdbc;
  private final String serviceKey;
  public InternalIncidentController(JdbcTemplate jdbc,
      @Value("${coldtrace.internal.service-key:}") String serviceKey) {
    this.jdbc = jdbc; this.serviceKey = serviceKey;
  }
  private void authorize(String candidate, long organizationId) {
    if (serviceKey.length() < 32) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
    if (candidate == null || !MessageDigest.isEqual(serviceKey.getBytes(StandardCharsets.UTF_8),
        candidate.getBytes(StandardCharsets.UTF_8))) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    if (organizationId < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  }
  @GetMapping
  public List<IncidentSnapshot> list(@PathVariable long organizationId,
      @RequestHeader(value = "X-Service-Key", required = false) String key) {
    authorize(key, organizationId);
    return jdbc.query("SELECT id,organization_id,asset_id,status,detected_at FROM incidents WHERE organization_id=? ORDER BY id",
        (row, index) -> new IncidentSnapshot(row.getLong("id"), row.getLong("organization_id"),
            row.getObject("asset_id", Long.class), row.getString("status"), row.getTimestamp("detected_at").toInstant()), organizationId);
  }
  @GetMapping("/{incidentId}")
  public IncidentSnapshot detail(@PathVariable long organizationId, @PathVariable long incidentId,
      @RequestHeader(value = "X-Service-Key", required = false) String key) {
    authorize(key, organizationId);
    var rows = jdbc.query("SELECT id,organization_id,asset_id,status,detected_at FROM incidents WHERE organization_id=? AND id=?",
        (row, index) -> new IncidentSnapshot(row.getLong("id"), row.getLong("organization_id"),
            row.getObject("asset_id", Long.class), row.getString("status"), row.getTimestamp("detected_at").toInstant()), organizationId, incidentId);
    return rows.stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }
}
