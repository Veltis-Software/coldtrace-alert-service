package com.acme.coldtrace.alerts.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.coldtrace.alerts.IncidentConsumer;
import com.acme.coldtrace.alerts.infrastructure.IdentityGateway;
import com.acme.coldtrace.shared.IdentitySession;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@CucumberContextConfiguration
@ActiveProfiles("test")
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:bdd;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver"
    })
@AutoConfigureMockMvc
public class IncidentLifecycleSteps {
  private static final Instant NOW = Instant.parse("2026-11-20T15:00:00Z");
  private static final Map<String, Long> USERS = Map.of("David", 10L, "Renso", 11L);

  @Autowired MockMvc mvc;
  @Autowired IncidentConsumer consumer;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @MockBean IdentityGateway identity;
  @MockBean Clock clock;

  private long incidentId;
  private ResultActions last;

  @Given("an operator {string} and a colleague {string} in organization {int}")
  public void users(String operator, String colleague, int organizationId) {
    for (String table :
        List.of(
            "corrective_actions",
            "alert_updates",
            "notifications",
            "outbox_events",
            "incidents",
            "processed_events")) jdbc.update("DELETE FROM " + table);
    var actor =
        new IdentitySession(USERS.get(operator), (long) organizationId, operator, List.of("alerts:update"));
    when(identity.session("Bearer " + operator)).thenReturn(actor);
    when(identity.session("Bearer reader"))
        .thenReturn(new IdentitySession(30L, (long) organizationId, "Reader", List.of()));
    when(identity.members("Bearer " + operator, actor))
        .thenReturn(
            List.of(
                new IdentityGateway.UserRef(USERS.get(operator), operator),
                new IdentityGateway.UserRef(USERS.get(colleague), colleague)));
    when(clock.instant()).thenReturn(NOW);
  }

  @And(
      "a CRITICAL threshold breach of {int} degrees for asset {string} with a safe range of {int} to {int} degrees")
  public void breach(int temperature, String assetName, int min, int max) {
    consumer.accept(
        json.valueToTree(
            Map.of(
                "eventId", UUID.randomUUID(),
                "eventType", "threshold.breached",
                "version", 1,
                "organizationId", 1,
                "occurredAt", NOW.toString(),
                "payload",
                    Map.of(
                        "assetId", 1,
                        "readingId", 2,
                        "severity", "CRITICAL",
                        "assetName", assetName,
                        "temperature", temperature,
                        "recordedAt", NOW.toString(),
                        "safeMinTemperature", min,
                        "safeMaxTemperature", max))));
    incidentId = jdbc.queryForObject("SELECT id FROM incidents", Long.class);
  }

  @Then("one OPEN incident and one notification exist for organization {int}")
  public void incidentOpened(int organizationId) {
    assertThat(jdbc.queryForObject("SELECT status FROM incidents", String.class)).isEqualTo("OPEN");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications", Long.class)).isEqualTo(1);
  }

  @When("{string} opens the alert detail")
  public void openDetail(String user) throws Exception {
    last = mvc.perform(get("/api/v1/alerts/{id}/detail", incidentId).header("Authorization", "Bearer " + user));
  }

  @Then("the detail shows asset {string}, a reading of {int} degrees and a maximum of {int} degrees")
  public void detail(String asset, int temperature, int max) throws Exception {
    last.andExpect(status().isOk())
        .andExpect(jsonPath("$.asset.name").value(asset))
        .andExpect(jsonPath("$.reading.temperature").value(temperature))
        .andExpect(jsonPath("$.safeRange.maxTemperature").value(max));
  }

  @When("{string} acknowledges the alert and assigns it to {string}")
  public void acknowledge(String user, String assignee) throws Exception {
    last =
        mvc.perform(
            post("/api/v1/alerts/{id}/acknowledgement", incidentId)
                .header("Authorization", "Bearer " + user)
                .contentType("application/json")
                .content("{\"assigneeUserId\":" + USERS.get(assignee) + "}"));
  }

  @Then("the incident state is ACKNOWLEDGED and the assignee is {string}")
  public void acknowledged(String assignee) throws Exception {
    last.andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("ACKNOWLEDGED"))
        .andExpect(jsonPath("$.assignee.userId").value(USERS.get(assignee)));
  }

  @When("a reader acknowledges the alert")
  public void readerAcknowledges() throws Exception {
    last =
        mvc.perform(
            post("/api/v1/alerts/{id}/acknowledgement", incidentId)
                .header("Authorization", "Bearer reader")
                .contentType("application/json")
                .content("{\"assigneeUserId\":11}"));
  }

  @Then("the request is rejected as forbidden and the incident remains OPEN")
  public void forbidden() throws Exception {
    last.andExpect(status().isForbidden());
    assertThat(jdbc.queryForObject("SELECT status FROM incidents", String.class)).isEqualTo("OPEN");
  }

  @When("{string} records the corrective action {string}")
  public void correctiveAction(String user, String description) throws Exception {
    mvc.perform(
            post("/api/v1/incidents/{id}/corrective-actions", incidentId)
                .header("Authorization", "Bearer " + user)
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("description", description))))
        .andExpect(status().isCreated());
  }

  @When("{string} resolves the incident with {string}")
  public void resolve(String user, String description) throws Exception {
    last =
        mvc.perform(
            post("/api/v1/incidents/{id}/resolution", incidentId)
                .header("Authorization", "Bearer " + user)
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("description", description))));
    last.andExpect(status().isOk());
  }

  @Then("the incident state is RESOLVED")
  public void resolved() throws Exception {
    last.andExpect(jsonPath("$.state").value("RESOLVED"));
  }

  @And("the corrective actions are authored by {string}")
  public void authoredBy(String user) {
    List<Long> authors = jdbc.queryForList("SELECT author_user_id FROM corrective_actions", Long.class);
    assertThat(authors).isNotEmpty().allMatch(USERS.get(user)::equals);
  }

  @And("the incident history is filtered by state {word} and severity {word}")
  public void filterHistory(String state, String severity) throws Exception {
    last = history("state", state, "severity", severity);
  }

  @Then("the history contains {int} incident(s)")
  public void historySize(int size) throws Exception {
    last.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(size));
  }

  @And("filtering the history by state {word} returns {int} incidents")
  public void emptyHistory(String state, int size) throws Exception {
    history("state", state).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(size));
  }

  private ResultActions history(String... params) throws Exception {
    var request =
        get("/api/v1/incidents/traceability")
            .header("Authorization", "Bearer David")
            .param("from", NOW.minusSeconds(3600).toString())
            .param("to", NOW.plusSeconds(3600).toString());
    for (int i = 0; i < params.length; i += 2) request.param(params[i], params[i + 1]);
    return mvc.perform(request);
  }
}
