package com.acme.coldtrace.alerts;

import com.acme.coldtrace.alerts.infrastructure.IdentityGateway;
import com.acme.coldtrace.shared.IdentitySession;
import com.acme.coldtrace.platform.alerts.application.commandservices.AiResolutionPlanCommandService;
import com.acme.coldtrace.platform.alerts.domain.model.commands.CreateAiResolutionPlanCommand;
import com.acme.coldtrace.platform.alerts.domain.model.valueobjects.AiResolutionPlanStep;
import com.acme.coldtrace.platform.iam.interfaces.acl.IamContextFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ActiveProfiles("test")
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:aiPlans;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver"})
@AutoConfigureMockMvc
class IncidentAiPlanTransactionTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired AiResolutionPlanCommandService commands;
  @MockBean IdentityGateway identity;
  @MockBean IamContextFacade organizations;
  @MockBean Clock clock;
  long plan;
  @BeforeEach void fixture() {
    for (var table : List.of("incident_ai_resolution_plan_steps","incident_ai_resolution_plan_required_evidences",
        "incident_ai_resolution_plan_uncertainty_notes","incident_ai_resolution_plans","corrective_actions",
        "alert_updates","notifications","outbox_events","incidents","processed_events")) jdbc.update("DELETE FROM "+table);
    when(clock.instant()).thenReturn(Instant.parse("2026-11-05T06:00:00Z"));
    when(organizations.organizationExists(1L)).thenReturn(true);
    when(identity.session("Bearer actor")).thenReturn(new IdentitySession(7L,1L,"Mariana Vargas",List.of("alerts:update")));
    when(identity.session("Bearer foreign")).thenReturn(new IdentitySession(8L,2L,"Lucia Salazar",List.of("alerts:update")));
    when(identity.session("Bearer reader")).thenReturn(new IdentitySession(9L,1L,"Valeria Rojas",List.of("assets:view")));
    jdbc.update("INSERT INTO incidents(id,organization_id,asset_id,source_event_id,type,severity,status,detected_at) VALUES(1,1,20,?,'THRESHOLD_BREACHED','CRITICAL','OPEN',CURRENT_TIMESTAMP)",UUID.randomUUID().toString());
    plan=commands.handle(new CreateAiResolutionPlanCommand(1L,1L,"Variacion termica",
        "Sonda pendiente de calibracion",List.of(new AiResolutionPlanStep(1,"Revisar sonda","Confirmar medicion","Lectura contrastada")),
        "Calibrar sonda","Verificar estabilidad",false,"none","Sin escalamiento",
        List.of("Registro de calibracion"),List.of("Requiere verificacion presencial"),"ollama","gemma3:4b","fixture"))
        .success().orElseThrow().getId();
  }
  private String path() {return "/api/v1/organizations/1/incidents/1/ai-resolution-plans/"+plan+"/approvals";}
  private String payload() throws Exception {
    return json.writeValueAsString(Map.of("approvedBy","Nombre enviado por el navegador",
        "finalCorrectiveAction","Sonda calibrada y contrastada","finalResolutionNotes","Temperatura estable en rango"));
  }
  private int approve(String bearer) throws Exception {
    return mvc.perform(post(path()).header("Authorization",bearer).contentType("application/json").content(payload())).andReturn().getResponse().getStatus();
  }
  @Test void approvalAndResolutionCommitTogetherUsingTheAuthenticatedActor() throws Exception {
    assertEquals(200,approve("Bearer actor"));
    assertEquals("APPROVED",jdbc.queryForObject("SELECT status FROM incident_ai_resolution_plans WHERE id=?",String.class,plan));
    assertEquals("Mariana Vargas",jdbc.queryForObject("SELECT approved_by FROM incident_ai_resolution_plans WHERE id=?",String.class,plan));
    assertEquals("RESOLVED",jdbc.queryForObject("SELECT status FROM incidents WHERE id=1",String.class));
    assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM corrective_actions WHERE incident_id=1",Integer.class));
    var response=mvc.perform(get("/api/v1/organizations/1/incidents").header("Authorization","Bearer actor"))
        .andReturn().getResponse();
    assertEquals(200,response.getStatus());
    assertEquals("resolved",json.readTree(response.getContentAsString()).get(0).path("status").asText());
    assertEquals(403,mvc.perform(get("/api/v1/organizations/2/incidents").header("Authorization","Bearer actor"))
        .andReturn().getResponse().getStatus());
  }
  @Test void failedPlanPersistenceRollsBackTheIncidentActionsAndStreamUpdate() throws Exception {
    jdbc.execute("ALTER TABLE incident_ai_resolution_plans ADD CONSTRAINT reject_approval CHECK(status<>'APPROVED')");
    try {assertThrows(Exception.class,()->approve("Bearer actor"));}
    finally {jdbc.execute("ALTER TABLE incident_ai_resolution_plans DROP CONSTRAINT reject_approval");}
    assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM incident_ai_resolution_plans WHERE id=?",String.class,plan));
    assertEquals("OPEN",jdbc.queryForObject("SELECT status FROM incidents WHERE id=1",String.class));
    assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM corrective_actions",Integer.class));
    assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM alert_updates",Integer.class));
  }
  @Test void concurrentApprovalsProduceOneDecisionAndOneResolution() throws Exception {
    var pool=Executors.newFixedThreadPool(2);
    var start=new CountDownLatch(1);
    try {
      Callable<Integer> request=()->{start.await();return approve("Bearer actor");};
      var first=pool.submit(request);var second=pool.submit(request);start.countDown();
      var statuses=List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS));
      assertTrue(statuses.contains(200));assertTrue(statuses.contains(409));
      assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM corrective_actions",Integer.class));
      assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM alert_updates",Integer.class));
    } finally {pool.shutdownNow();}
  }
  @Test void foreignOrganizationAndReadOnlyRoleCannotDecide() throws Exception {
    assertEquals(403,approve("Bearer foreign"));assertEquals(403,approve("Bearer reader"));
    assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM incident_ai_resolution_plans WHERE id=?",String.class,plan));
    assertEquals("OPEN",jdbc.queryForObject("SELECT status FROM incidents WHERE id=1",String.class));
  }
}
