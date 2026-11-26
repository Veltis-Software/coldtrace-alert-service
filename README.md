# ColdTrace Alert Service

Consumes `threshold.breached` and `source.gap` integration events. Incident,
notification, processed-event marker and outbox entry commit atomically.
Duplicate event delivery produces one incident. Gap recovery resolves only the
matching organization's asset; an older close cannot resolve a newer gap.

## API and notifications

Authenticated incident lists, detail and in-app notifications are scoped to the IAM session organization. Detail includes asset context, measured values, effective ranges, assignee and corrective actions. `POST /api/v1/alerts/{id}/acknowledgement` requires `alerts:update` and a real organization member; the author can undo with DELETE for ten seconds. Incident action and resolution endpoints preserve authorship and timestamps.

`GET /api/v1/alerts/stream` provides durable `alert.updated` SSE events, organization-scoped cursor replay and 25-second keepalives. Bearer tokens remain in headers. `incident.opened` is committed in the outbox and relayed through the explicitly enabled local Pub/Sub emulator. Notifications use the durable IN_APP channel; email/SMS is outside this increment.

## Configuration

| Variable | Purpose / default |
|---|---|
| PORT | HTTP port; 8084 |
| DB_URL, DB_USER, DB_PASSWORD | MySQL connection to `ct_alert` |
| IDENTITY_URL | IAM URL; http://127.0.0.1:8090 |
| COLDTRACE_PUBSUB_LOCAL_PUSH_ENABLED | Explicit emulator receiver opt-in; false |

When enabled locally, `POST /internal/pubsub/events` accepts Pub/Sub push
envelopes. It is disabled by default and has no cloud OIDC validation; enable
it only with the local emulator. Schema changes use Flyway.

OpenAPI: `/v3/api-docs`; Swagger: `/swagger-ui.html`; health:
`/actuator/health`. Tests cover duplicate delivery, rollback on notification
failure, gap closure isolation, invalid envelopes and delayed close events.

## Build and test

Requires Java 21 and Maven. Install the common library from the infrastructure repository first:

```sh
mvn -B -f ../coldtrace-infrastructure/libraries/coldtrace-shared/pom.xml install
mvn -B verify
```

Build the standalone image with the sibling library as a named build context:

```sh
docker build --build-context shared=../coldtrace-infrastructure/libraries/coldtrace-shared -t coldtrace-alert-service:dev .
```

The CI workflow installs the pinned shared source, runs tests, builds the image
and uploads test reports. Local orchestration is in `coldtrace-infrastructure`.
