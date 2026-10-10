package com.acme.coldtrace.alerts.domain;

import java.time.Instant;

public final class AcknowledgementPolicy {
  private AcknowledgementPolicy() {}

  public static boolean undoAvailable(String state, Instant acknowledgedAt, Instant now) {
    return "ACKNOWLEDGED".equals(state)
        && acknowledgedAt != null
        && !now.isBefore(acknowledgedAt)
        && !now.isAfter(acknowledgedAt.plusSeconds(10));
  }
}
