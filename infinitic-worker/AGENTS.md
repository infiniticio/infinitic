<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-worker

## Purpose

The production entry point for running all Infinitic server-side components in a single JVM
process. `InfiniticWorker` wires together the workflow state engine, workflow tag engine,
service task executors, service tag engines, and an optional CloudEvents event listener.
Each component subscribes to its own topic(s) with configurable concurrency and processes
messages using coroutines. `InfiniticWorkerBuilder` provides a fluent API for programmatic
construction; `InfiniticWorkerConfig` supports YAML-based configuration.

## Key Files

| File | Description |
|------|-------------|
| `InfiniticWorker.kt` | Main class. Subscribes to all required topics (service executor, workflow state, workflow tag, service tag, retry, event topics) and starts coroutine-based consumers. Implements `AutoCloseable`. |
| `InfiniticWorkerBuilder.kt` | Fluent builder for programmatic worker setup. Accepts `TransportConfig`, `StorageConfig`, `LogsConfig`, and per-service/per-workflow executor/tag-engine configs. |
| `config/InfiniticWorkerConfig.kt` | Data class for full worker configuration. Validates storage is set for any engine that needs it. Loaded from YAML file, resource, or string. |
| `config/InfiniticWorkerConfigInterface.kt` | Interface for worker config. |
| `config/ConfigGetterInterface.kt` | Interface exposing config accessors used by worker internals. |
| `config/LogsConfig.kt` | Configuration for log level and format. |
| `config/ServiceConfig.kt` | Aggregates `ServiceExecutorConfig` and `ServiceTagEngineConfig` for one service. |
| `config/ServiceExecutorConfig.kt` | Configures concurrency, factory, `WithTimeout`, `WithRetry` for a service executor. |
| `config/ServiceTagEngineConfig.kt` | Configures storage and concurrency for the service tag engine. |
| `config/WorkflowConfig.kt` | Aggregates `WorkflowExecutorConfig`, `WorkflowStateEngineConfig`, and `WorkflowTagEngineConfig` for one workflow. |
| `config/WorkflowExecutorConfig.kt` | Configures concurrency, versioned workflow factories, `WithTimeout`, `WithRetry`, and `checkMode`. |
| `config/WorkflowStateEngineConfig.kt` | Configures storage and concurrency for the workflow state engine. |
| `config/WorkflowTagEngineConfig.kt` | Configures storage and concurrency for the workflow tag engine. |
| `registry/ExecutorRegistry.kt` | Implements `ExecutorRegistryInterface`. Resolves service/workflow instances, retry/timeout policies, and `WorkflowCheckMode` from config at execution time. Manages versioned workflow factories. |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `config/` | All configuration data classes and interfaces for the worker and its sub-components. |
| `registry/` | Runtime registry that maps service/workflow names to instances and policies. |

## For AI Agents

### Working In This Directory

- `InfiniticWorker` is the aggregation point: it instantiates `TaskExecutor`,
  `TaskEventHandler`, `TaskRetryHandler`, `TaskTagEngine`, and the workflow engine components,
  then connects each to the appropriate topic consumer.
- Storage (`StorageConfig`) is required for any engine (workflow state engine, workflow tag
  engine, service tag engine). If a config declares an engine without storage, the
  `InfiniticWorkerConfig` constructor throws `IllegalArgumentException`.
- Worker-level `storage` acts as the default; per-engine storage overrides it when set.
- `ExecutorRegistry` resolves workflow instances by version. Versioned factories are
  registered in `WorkflowExecutorConfig.factories` (a list in reverse-version order). The
  most recent version is resolved at dispatch time when no explicit version is requested.
- The worker creates an internal `InfiniticClient` for delegated task completion and for
  forwarding workflow task results.
- Topic subscriptions and concurrency levels are configured per component. Default concurrency
  is defined in `UNSET_CONCURRENCY` and resolved per-engine.
- Batch processing is supported for service executor topics when a `@Batch` method is found.

### Testing Requirements

- Tests use Hoplite YAML loading (`testImplementation(Libs.Hoplite.core/yaml)`) to exercise
  config parsing from files.
- Integration tests live in `infinitic-tests` and require a running transport (Pulsar or
  in-memory).
- Unit test target: `./gradlew :infinitic-worker:test`

### Common Patterns

- Programmatic setup:
  ```kotlin
  val worker = InfiniticWorker.builder()
      .setTransport(transportConfig)
      .setStorage(storageConfig)
      .addServiceExecutor(ServiceExecutorConfig.builder()
          .setServiceName("MyService")
          .setFactory { MyServiceImpl() }
          .build())
      .build()
  worker.start()
  ```
- YAML setup: `InfiniticWorker.fromYamlFile("infinitic.yml")`
- Adding a workflow: call `addWorkflowExecutor` and `addWorkflowStateEngine` on the builder.
- Adding an event listener: call `setEventListener(EventListenerConfig(...))`.

## Dependencies

### Internal

| Module | Role |
|--------|------|
| `infinitic-client` | Embedded client used internally by the worker |
| `infinitic-common` | All message types, transport interfaces, serialization |
| `infinitic-task-executor` | `TaskExecutor`, `TaskEventHandler`, `TaskRetryHandler` |
| `infinitic-task-tag` | `TaskTagEngine` |
| `infinitic-workflow-engine` | Workflow state engine |
| `infinitic-workflow-tag` | Workflow tag engine |
| `infinitic-workflow-task` | Workflow task execution support |
| `infinitic-storage` | Storage abstractions and implementations |
| `infinitic-transport` | Transport config and abstractions |
| `infinitic-cloudevents` | CloudEvents event listener integration |
| `infinitic-utils` | Miscellaneous utilities |

### External

| Library | Role |
|---------|------|
| `kotlin-reflect` | Reflection used by executor registry |
| `kotlinx-coroutines-core` | Coroutine-based consumer loops |
| `kotlinx-coroutines-jdk8` | CompletableFuture integration |
| `cloudevents-core` | CloudEvents message format |
