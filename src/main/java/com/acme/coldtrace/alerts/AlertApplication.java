package com.acme.coldtrace.alerts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages={"com.acme.coldtrace.alerts", "com.acme.coldtrace.platform.alerts"})
@org.springframework.boot.autoconfigure.domain.EntityScan("com.acme.coldtrace.platform.alerts.infrastructure.persistence.jpa.entities")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories("com.acme.coldtrace.platform.alerts.infrastructure.persistence.jpa.repositories")
@org.springframework.data.jpa.repository.config.EnableJpaAuditing
@org.springframework.scheduling.annotation.EnableScheduling
public class AlertApplication {
  @org.springframework.context.annotation.Bean
  java.time.Clock clock() {
    return java.time.Clock.systemUTC();
  }

  public static void main(String[] args) {
    SpringApplication.run(AlertApplication.class, args);
  }
}
