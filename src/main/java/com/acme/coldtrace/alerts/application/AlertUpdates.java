package com.acme.coldtrace.alerts.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Durable stream records share the incident transaction; every instance reads its own clients. */
@Component
public class AlertUpdates {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;

  public AlertUpdates(JdbcTemplate jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public void append(long id, long org, long asset, String state, Long assignee, Instant at) {
    var event = new LinkedHashMap<String, Object>();
    var eventId = UUID.randomUUID().toString();
    event.put("eventId", eventId);
    event.put("alertId", id);
    event.put("assetId", asset);
    event.put("state", state);
    event.put("assigneeUserId", assignee);
    event.put("occurredAt", at);
    try {
      jdbc.update(
          "INSERT INTO alert_updates(event_id,organization_id,incident_id,payload,occurred_at)"
              + " VALUES (?,?,?,?,?)",
          eventId,
          org,
          id,
          json.writeValueAsString(event),
          Timestamp.from(at));
    } catch (Exception e) {
      throw new IllegalStateException("Cannot persist alert update", e);
    }
  }
}
