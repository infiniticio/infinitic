<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-client

## Purpose
Provides the public API for interacting with Infinitic from application code. Users create an `InfiniticClient` to dispatch workflows and tasks, retrieve workflow state, send signals, cancel workflows, and await results. The client communicates with the workflow engine and task executors via the configured transport layer.

## Key Files

| File | Description |
|------|-------------|
| `src/main/kotlin/io/infinitic/clients/InfiniticClient.kt` | Main client interface and implementation — `dispatch`, `cancel`, `signal`, `getState`, `await` |
| `src/main/kotlin/io/infinitic/clients/dispatcher/ClientDispatcher.kt` | Sends messages to workflow engine and task executors; handles response correlation |
| `src/main/kotlin/io/infinitic/clients/dispatcher/ResponseFlow.kt` | Coroutine flow that collects responses from the client response topic |
| `src/main/kotlin/io/infinitic/clients/config/InfiniticClientConfig.kt` | Client configuration — transport, client name |
| `src/main/kotlin/io/infinitic/clients/config/InfiniticClientConfigInterface.kt` | Interface for client config (implemented by `InfiniticWorkerConfig` too) |
| `src/main/kotlin/io/infinitic/clients/deferred/NewDeferredWorkflow.kt` | Represents a newly dispatched workflow (has workflow ID, can await result) |
| `src/main/kotlin/io/infinitic/clients/deferred/ExistingDeferredWorkflow.kt` | Represents an already-running workflow (looked up by ID or tag) |
| `src/main/kotlin/io/infinitic/clients/deferred/DeferredChannel.kt` | Represents a signal channel on a deferred workflow |
| `src/main/kotlin/io/infinitic/clients/deferred/DeferredSend.kt` | Represents a dispatched task (no result awaiting) |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `src/main/kotlin/io/infinitic/clients/config/` | Client configuration classes |
| `src/main/kotlin/io/infinitic/clients/deferred/` | Deferred result types for dispatched workflows/tasks |
| `src/main/kotlin/io/infinitic/clients/dispatcher/` | Message dispatch and response correlation |

## For AI Agents

### Working In This Directory
- `InfiniticClient` is the primary user-facing API — changes here are breaking changes
- Client creates a stub (proxy) of the workflow/service interface using `infinitic-common` proxies
- Response correlation: client sends a message with a unique `clientName` and listens on a dedicated response topic
- `await()` on a `DeferredWorkflow` blocks (suspends) until the workflow completes

### Testing Requirements
- `./gradlew :infinitic-client:test`
- Tests use in-memory transport

### Common Patterns
- Workflow dispatch: `client.dispatch(MyWorkflow::class.java) { myMethod(args) }`
- Task dispatch: `client.dispatch(MyService::class.java) { myMethod(args) }`
- Await: `deferred.await()` — blocks until result is available

## Dependencies

### Internal
- `infinitic-common` — message types, proxy utilities, transport interfaces
- `infinitic-utils` — config loading

### External
- Kotlin Coroutines — async dispatch and await
- kotlinx.serialization — message serialization

<!-- MANUAL: -->
