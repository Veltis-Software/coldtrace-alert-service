package com.acme.coldtrace.platform.alerts.application.ports;

import java.time.Instant;

public record IncidentEvidenceSnapshot(
    Long id,
    Long organizationId,
    Long assetId,
    Long deviceId,
    Long readingId,
    String assetName,
    String deviceName,
    String type,
    String severity,
    String status,
    String value,
    Instant detectedAt,
    Instant acknowledgedAt,
    String acknowledgedBy,
    Instant escalatedAt,
    String escalatedBy,
    String escalationReason,
    Instant correctiveActionRegisteredAt,
    String correctiveActionRegisteredBy,
    String correctiveAction
) {
  public Long getId() { return id; }
  public Long getOrganizationId() { return organizationId; }
  public Long getAssetId() { return assetId; }
  public Long getDeviceId() { return deviceId; }
  public Long getReadingId() { return readingId; }
  public String getAssetName() { return assetName; }
  public String getDeviceName() { return deviceName; }
  public String getType() { return type; }
  public String getSeverity() { return severity; }
  public String getStatus() { return status; }
  public String getValue() { return value; }
  public Instant getDetectedAt() { return detectedAt; }
  public Instant getAcknowledgedAt() { return acknowledgedAt; }
  public String getAcknowledgedBy() { return acknowledgedBy; }
  public Instant getEscalatedAt() { return escalatedAt; }
  public String getEscalatedBy() { return escalatedBy; }
  public String getEscalationReason() { return escalationReason; }
  public Instant getCorrectiveActionRegisteredAt() { return correctiveActionRegisteredAt; }
  public String getCorrectiveActionRegisteredBy() { return correctiveActionRegisteredBy; }
  public String getCorrectiveAction() { return correctiveAction; }
  public boolean isOpen() { return "OPEN".equalsIgnoreCase(status); }
  public boolean isAcknowledged() { return "ACKNOWLEDGED".equalsIgnoreCase(status); }
}
