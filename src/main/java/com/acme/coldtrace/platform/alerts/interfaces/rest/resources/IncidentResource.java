package com.acme.coldtrace.platform.alerts.interfaces.rest.resources;

import java.time.Instant;

/**
 * Response resource representing an incident.
 *
 * @param id incident identifier
 * @param organizationId organization identifier
 * @param assetId optional asset identifier
 * @param deviceId optional device identifier
 * @param readingId optional sensor reading identifier
 * @param assetName optional asset display name
 * @param deviceName optional device display name
 * @param type incident type
 * @param severity incident severity
 * @param status incident lifecycle status
 * @param value detected or reported value
 * @param detectedAt detection timestamp
 * @param acknowledgedAt acknowledgement timestamp
 * @param acknowledgedBy actor that acknowledged the incident
 * @param escalatedAt escalation timestamp
 * @param escalatedBy actor that escalated the incident
 * @param escalationReason reason that justified escalation
 * @param correctiveActionRegisteredAt corrective action registration timestamp
 * @param correctiveActionRegisteredBy actor that registered the corrective action
 * @param correctiveAction corrective action details
 * @param resolvedAt resolution timestamp
 * @param resolvedBy actor that resolved the incident
 * @param resolutionNotes resolution notes
 * @param lastNotificationStatus last derived notification status
 * @param lastNotificationAt last derived notification timestamp
 * @param notificationCount number of notifications emitted for this incident
 * @since 1.0
 */
public record IncidentResource(
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
        String correctiveAction,
        Instant resolvedAt,
        String resolvedBy,
        String resolutionNotes,
        String lastNotificationStatus,
        Instant lastNotificationAt,
        Integer notificationCount
) {
}
