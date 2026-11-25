package com.acme.coldtrace.alerts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
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
