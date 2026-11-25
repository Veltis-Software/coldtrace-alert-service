package com.acme.coldtrace.platform.alerts.interfaces.rest.resources;

import java.time.Instant;

/**
 * Response resource representing an incident notification read model.
 *
 * @param id notification identifier
 * @param organizationId organization identifier
 * @param incidentId incident identifier
 * @param channel notification channel
 * @param recipient notification recipient
 * @param message notification message
 * @param status notification status
 * @param createdAt creation timestamp
 * @param deliveredAt delivery timestamp
 * @param failureReason delivery failure reason
 * @since 1.0
 */
public record NotificationResource(
        Long id,
        Long organizationId,
        Long incidentId,
        String channel,
        String recipient,
        String message,
        String status,
        Instant createdAt,
        Instant deliveredAt,
        String failureReason
) {
}
