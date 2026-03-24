<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-cloudevents

## Purpose
CloudEvents specification integration for Infinitic. Converts internal Infinitic events (task started/completed/failed, workflow started/completed/failed) into CloudEvents-formatted JSON and delivers them to a configurable listener. Enables interoperability with event-driven systems, observability platforms, and audit logging.

## Key Files

| File | Description |
|------|-------------|
| `src/main/kotlin/io/infinitic/events/toCloudEvent.kt` | Converts Infinitic messages to CloudEvents format |
| `src/main/kotlin/io/infinitic/events/toJsonString.kt` | Serializes CloudEvents to JSON strings |
| `src/main/kotlin/io/infinitic/events/CloudEventLogger.kt` | Delivers CloudEvents to the configured listener (e.g., HTTP, logging) |
| `src/main/kotlin/io/infinitic/events/config/EventListenerConfig.kt` | Configuration for the event listener (endpoint, batch size, etc.) |
| `src/main/kotlin/io/infinitic/events/types/types.kt` | CloudEvent type constants for all Infinitic event types |
| `src/main/kotlin/io/infinitic/events/data/services/serviceData.kt` | CloudEvent data payload for service task events |
| `src/main/kotlin/io/infinitic/events/data/services/serviceTypes.kt` | Service-specific CloudEvent type definitions |
| `src/main/kotlin/io/infinitic/events/data/workflows/workflowData.kt` | CloudEvent data payload for workflow events |
| `src/main/kotlin/io/infinitic/events/data/workflows/workflowTypes.kt` | Workflow-specific CloudEvent type definitions |
| `src/main/kotlin/io/infinitic/events/errors/errors.kt` | Error data structures for CloudEvent payloads |
| `src/main/kotlin/io/infinitic/events/listeners/startCloudEventListener.kt` | Starts the cloud event listener coroutine |
| `src/main/kotlin/io/infinitic/events/listeners/listenToServiceExecutorTopics.kt` | Subscribes to service executor topics and emits CloudEvents |
| `src/main/kotlin/io/infinitic/events/listeners/listenToWorkflowExecutorTopics.kt` | Subscribes to workflow executor topics and emits CloudEvents |
| `src/main/kotlin/io/infinitic/events/listeners/listenToWorkflowStateTopics.kt` | Subscribes to workflow state topics and emits CloudEvents |
| `src/main/kotlin/io/infinitic/events/listeners/refreshServiceListAsync.kt` | Dynamically discovers new services and subscribes to their topics |
| `src/main/kotlin/io/infinitic/events/listeners/refreshWorkflowListAsync.kt` | Dynamically discovers new workflows and subscribes to their topics |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `src/main/kotlin/io/infinitic/events/config/` | Event listener configuration |
| `src/main/kotlin/io/infinitic/events/data/` | CloudEvent data payload definitions (services, workflows) |
| `src/main/kotlin/io/infinitic/events/errors/` | Error payload types |
| `src/main/kotlin/io/infinitic/events/listeners/` | Topic subscription and event emission coroutines |
| `src/main/kotlin/io/infinitic/events/types/` | CloudEvent type string constants |

## For AI Agents

### Working In This Directory
- CloudEvents spec: each event has `type`, `source`, `id`, `time`, `datacontenttype`, and `data` fields
- The listener is a read-only observer — it does not affect workflow or task execution
- Dynamic topic discovery (`refreshServiceListAsync`, `refreshWorkflowListAsync`) polls for new service/workflow registrations

### Testing Requirements
- `./gradlew :infinitic-cloudevents:test`
- Test fixtures in `src/test/resources/` contain expected CloudEvent JSON

### Common Patterns
- CloudEvent types follow pattern: `io.infinitic.{entity}.{event}` (e.g., `io.infinitic.task.completed`)
- All events include `workflowId` or `taskId` as CloudEvent subject

## Dependencies

### Internal
- `infinitic-common` — message types, transport consumer interfaces

### External
- CloudEvents Java SDK — CloudEvent data structures
- kotlinx.serialization — JSON serialization
- Kotlin Coroutines

<!-- MANUAL: -->
