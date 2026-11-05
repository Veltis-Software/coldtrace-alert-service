package com.acme.coldtrace.alerts;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class InternalIncidentControllerTest {
  private static final String KEY = "isolated-owner-contract-key-at-least-32-characters";
  private InternalIncidentController controller;
  @BeforeEach void ownerDatabase() {
    var jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    jdbc.execute("CREATE TABLE incidents(id BIGINT, organization_id BIGINT, asset_id BIGINT, status VARCHAR(32), detected_at TIMESTAMP)");
    jdbc.update("INSERT INTO incidents VALUES (1,7,20,'OPEN',CURRENT_TIMESTAMP),(2,8,30,'RESOLVED',CURRENT_TIMESTAMP)");
    controller = new InternalIncidentController(jdbc, KEY);
  }
  @Test void workloadReadNeverLeaksAnotherOrganizationsIncident() {
    var rows = controller.list(7, KEY);
    assertEquals(1, rows.size());
    assertEquals(20L, rows.getFirst().assetId());
    assertEquals(404, assertThrows(ResponseStatusException.class,
        () -> controller.detail(7, 2, KEY)).getStatusCode().value());
  }
  @Test void browserBearerIsNotAnInternalCredential() {
    assertEquals(401, assertThrows(ResponseStatusException.class,
        () -> controller.list(7, "Bearer browser-token")).getStatusCode().value());
  }
  @Test void missingWorkloadConfigurationFailsClosed() {
    var disabled = new InternalIncidentController(null, "");
    assertEquals(503, assertThrows(ResponseStatusException.class,
        () -> disabled.list(7, KEY)).getStatusCode().value());
  }
}
