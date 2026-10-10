package com.acme.coldtrace.alerts.infrastructure;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Database cursor replay works across service instances and reconnects. No JWT in URLs. */
@Component
public class AlertStream {
  private final JdbcTemplate jdbc;
  private final IdentityGateway identity;
  private final Map<SseEmitter, Client> clients = new ConcurrentHashMap<>();

  private static final class Client {
    final long org;
    final String bearer;
    long cursor;
    Instant heartbeat = Instant.now();

    Client(long org, String bearer, long cursor) {
      this.org = org;
      this.bearer = bearer;
      this.cursor = cursor;
    }
  }

  public AlertStream(JdbcTemplate jdbc, IdentityGateway identity) {
    this.jdbc = jdbc;
    this.identity = identity;
  }

  public SseEmitter open(long org, String bearer, String lastEventId) {
    long cursor;
    if (lastEventId != null && !lastEventId.isBlank()) {
      try {
        UUID.fromString(lastEventId);
      } catch (IllegalArgumentException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid event cursor");
      }
      var rows =
          jdbc.queryForList(
              "SELECT sequence_id FROM alert_updates WHERE event_id=? AND organization_id=?",
              Long.class,
              lastEventId,
              org);
      if (rows.isEmpty())
        throw new ResponseStatusException(HttpStatus.CONFLICT, "STREAM_CURSOR_UNKNOWN");
      cursor = rows.get(0);
    } else
      cursor =
          jdbc.queryForObject(
              "SELECT COALESCE(MAX(sequence_id),0) FROM alert_updates WHERE organization_id=?",
              Long.class,
              org);
    var emitter = new SseEmitter(3_600_000L);
    clients.put(emitter, new Client(org, bearer, cursor));
    emitter.onCompletion(() -> clients.remove(emitter));
    emitter.onTimeout(() -> clients.remove(emitter));
    emitter.onError(e -> clients.remove(emitter));
    try {
      emitter.send(SseEmitter.event().comment("connected"));
    } catch (Exception e) {
      clients.remove(emitter);
      emitter.completeWithError(e);
    }
    return emitter;
  }

  @Scheduled(fixedDelay = 500)
  public void poll() {
    clients.forEach(
        (emitter, client) -> {
          try {
            if (Instant.now().isAfter(client.heartbeat.plusSeconds(25))) {
              var session = identity.session(client.bearer);
              if (session.organizationId() != client.org)
                throw new IllegalStateException("Organization changed");
              emitter.send(SseEmitter.event().comment("ping"));
              client.heartbeat = Instant.now();
            }
            var rows =
                jdbc.queryForList(
                    "SELECT sequence_id,event_id,payload FROM alert_updates WHERE organization_id=?"
                        + " AND sequence_id>? ORDER BY sequence_id LIMIT 100",
                    client.org,
                    client.cursor);
            for (var row : rows) {
              var raw = row.get("payload");
              String data =
                  raw instanceof byte[] b
                      ? new String(b, java.nio.charset.StandardCharsets.UTF_8)
                      : raw.toString();
              emitter.send(
                  SseEmitter.event()
                      .id(row.get("event_id").toString())
                      .name("alert.updated")
                      .data(data));
              client.cursor = ((Number) row.get("sequence_id")).longValue();
            }
          } catch (Exception e) {
            clients.remove(emitter);
            emitter.completeWithError(e);
          }
        });
  }
}
