package com.acme.coldtrace.alerts;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.acme.coldtrace.alerts.infrastructure.IdentityGateway;
import com.acme.coldtrace.shared.IdentitySession;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:workflow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver"
    })
@AutoConfigureMockMvc
class AlertWorkflowTest {
  @Autowired MockMvc mvc;
  @Autowired IncidentConsumer consumer;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @MockBean IdentityGateway identity;
  @MockBean Clock clock;
  private final Instant now = Instant.parse("2026-10-09T22:00:00Z");
  long id;

  @BeforeEach
  void prepare() {
    for (String table :
        List.of(
            "corrective_actions",
            "alert_updates",
            "notifications",
            "outbox_events",
            "incidents",
            "processed_events")) jdbc.update("DELETE FROM " + table);
    var actor = new IdentitySession(10L, 1L, "David", List.of("alerts:update"));
    when(identity.session("Bearer actor")).thenReturn(actor);
    when(identity.session("Bearer other"))
        .thenReturn(new IdentitySession(20L, 2L, "Other", List.of("alerts:update")));
    when(identity.session("Bearer reader"))
        .thenReturn(new IdentitySession(30L, 1L, "Reader", List.of()));
    when(identity.members("Bearer actor", actor))
        .thenReturn(
            List.of(
                new IdentityGateway.UserRef(10L, "David"),
                new IdentityGateway.UserRef(11L, "Renso")));
    when(clock.instant()).thenReturn(now);
    consumer.accept(
        json.valueToTree(
            Map.of(
                "eventId",
                UUID.randomUUID(),
                "eventType",
                "threshold.breached",
                "version",
                1,
                "organizationId",
                1,
                "occurredAt",
                now.toString(),
                "payload",
                Map.of(
                    "assetId",
                    1,
                    "readingId",
                    2,
                    "severity",
                    "CRITICAL",
                    "assetName",
                    "Cold room",
                    "temperature",
                    12,
                    "recordedAt",
                    now.toString(),
                    "safeMinTemperature",
                    2,
                    "safeMaxTemperature",
                    8))));
    id = jdbc.queryForObject("SELECT id FROM incidents", Long.class);
  }

  @Test
  void recognizesAssignsAndUndoesAtomicallyWithinTenSeconds() throws Exception {
    mvc.perform(
            post("/api/v1/alerts/{id}/acknowledgement", id)
                .header("Authorization", "Bearer actor")
                .contentType("application/json")
                .content("{\"assigneeUserId\":11}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("ACKNOWLEDGED"))
        .andExpect(jsonPath("$.assignee.userId").value(11));
    mvc.perform(
            post("/api/v1/alerts/{id}/acknowledgement", id)
                .header("Authorization", "Bearer actor")
                .contentType("application/json")
                .content("{\"assigneeUserId\":11}"))
        .andExpect(status().isConflict());
    when(clock.instant()).thenReturn(now.plusSeconds(10));
    mvc.perform(
            delete("/api/v1/alerts/{id}/acknowledgement", id)
                .header("Authorization", "Bearer actor"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("OPEN"));
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alert_updates", Long.class)).isEqualTo(3);
  }

  @Test
  void expiredUndoCannotModifyAssignment() throws Exception {
    mvc.perform(
            post("/api/v1/alerts/{id}/acknowledgement", id)
                .header("Authorization", "Bearer actor")
                .contentType("application/json")
                .content("{\"assigneeUserId\":11}"))
        .andExpect(status().isOk());
    when(clock.instant()).thenReturn(now.plusSeconds(11));
    mvc.perform(
            delete("/api/v1/alerts/{id}/acknowledgement", id)
                .header("Authorization", "Bearer actor"))
        .andExpect(status().isConflict());
    assertThat(jdbc.queryForObject("SELECT status FROM incidents", String.class))
        .isEqualTo("ACKNOWLEDGED");
  }

  @Test
  void rejectsReadersForeignOrganizationsAndForeignAssignees() throws Exception {
    mvc.perform(
            post("/api/v1/alerts/{id}/acknowledgement", id)
                .header("Authorization", "Bearer reader")
                .contentType("application/json")
                .content("{\"assigneeUserId\":11}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/alerts/{id}/detail", id).header("Authorization", "Bearer other"))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/v1/alerts/{id}/acknowledgement", id)
                .header("Authorization", "Bearer actor")
                .contentType("application/json")
                .content("{\"assigneeUserId\":999}"))
        .andExpect(status().isBadRequest());
    assertThat(jdbc.queryForObject("SELECT status FROM incidents", String.class)).isEqualTo("OPEN");
  }

  @Test
  void detailReturnsReadingRangeContextAndSuggestedAssignee() throws Exception {
    mvc.perform(get("/api/v1/alerts/{id}/detail", id).header("Authorization", "Bearer actor"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.asset.name").value("Cold room"))
        .andExpect(jsonPath("$.reading.temperature").value(12))
        .andExpect(jsonPath("$.safeRange.maxTemperature").value(8))
        .andExpect(jsonPath("$.suggestedAssignee.userId").value(10));
  }

  @Test
  void resolutionKeepsAuthorAndCorrectiveEvidence() throws Exception {
    mvc.perform(
            post("/api/v1/incidents/{id}/resolution", id)
                .header("Authorization", "Bearer actor")
                .contentType("application/json")
                .content("{\"description\":\"Cooling restored and inspected\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("RESOLVED"));
    assertThat(jdbc.queryForObject("SELECT author_user_id FROM corrective_actions", Long.class))
        .isEqualTo(10);
  }
}
