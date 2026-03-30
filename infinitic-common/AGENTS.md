<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-common

## Purpose

Shared foundation module imported by every other Infinitic module. Defines the core contracts
that the entire system is built on: the `Message` interface, all message types for every
component, `WorkflowState` (the authoritative workflow execution record), the transport
abstraction (`Topic`, `InfiniticProducer`, `InfiniticConsumer`), serialization via Avro and
JSON, and the dynamic-proxy mechanism that turns user-defined service/workflow interfaces into
dispatchable stubs.

Nothing in this module depends on any other `infinitic-*` module. All other modules depend on
this one.

## Key Files

| File | Description |
|------|-------------|
| `src/main/kotlin/io/infinitic/common/messages/Message.kt` | Root interface: `messageId`, `emitterName`, `key()`, `entity()` |
| `src/main/kotlin/io/infinitic/common/workflows/engine/state/WorkflowState.kt` | Full mutable state for one workflow instance; serialized to/from Avro binary |
| `src/main/kotlin/io/infinitic/common/transport/Topic.kt` | Sealed class hierarchy of all topics; extension properties `.withoutDelay` and `.forWorkflow` |
| `src/main/kotlin/io/infinitic/common/transport/interfaces/InfiniticProducer.kt` | `sendTo(topic, after)` suspend extension; routes workflowTask messages to workflow topics |
| `src/main/kotlin/io/infinitic/common/transport/interfaces/InfiniticConsumer.kt` | Typed consumer wrapping `TransportConsumer<TransportMessage<M>>` |
| `src/main/kotlin/io/infinitic/common/transport/interfaces/TransportConsumer.kt` | Low-level interface: `receive()`, `batchReceive()`, `maxRedeliveryCount` |
| `src/main/kotlin/io/infinitic/common/serDe/SerializedData.kt` | Universal value container: bytes + `SerializedDataType` + meta; used for task/workflow args and return values |
| `src/main/kotlin/io/infinitic/common/serDe/avro/AvroSerDe.kt` | Avro encoding with schema fingerprint for forward/backward compatibility across versions |
| `src/main/kotlin/io/infinitic/common/requester/Requester.kt` | Sealed interface: `ClientRequester` or `WorkflowRequester`; identifies who dispatched a task or workflow |
| `src/main/kotlin/io/infinitic/common/proxies/ProxyHandler.kt` | Base `InvocationHandler` for all service/workflow stub proxies |
| `src/main/kotlin/io/infinitic/common/registry/ExecutorRegistryInterface.kt` | Contract for instantiating service/workflow executors with retry/timeout/check-mode config |
| `src/main/kotlin/io/infinitic/common/storage/Flushable.kt` | Marker interface for in-memory stores that can be flushed (used in tests) |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `clients/` | `ClientName`, `ClientMessage` sealed interface, `ClientEnvelope`, message type enum, and client-facing interfaces |
| `config/` | Hoplite-based configuration loaders for the whole system |
| `data/` | Primitive value types: `MessageId`, `MillisDuration`, `MillisInstant`, `Name`, `Version`, `Hash`; method data (`MethodName`, `MethodArgs`, `MethodParameterTypes`) |
| `emitters/` | `EmitterName` value type (identifies the component that produced a message) |
| `exceptions/` | `thisShouldNotHappen()` — assertion utility for unreachable branches |
| `messages/` | `Message` root interface only |
| `proxies/` | Dynamic-proxy handlers: `NewServiceProxyHandler`, `ExistingServiceProxyHandler`, `NewWorkflowProxyHandler`, `ExistingWorkflowProxyHandler`, `ChannelProxyHandler`, `ProxyDispatcher` |
| `registry/` | `ExecutorRegistryInterface` — pluggable factory for executor instances |
| `requester/` | `Requester` sealed interface (`ClientRequester`, `WorkflowRequester`) with extension properties |
| `serDe/` | `SerializedData`, `SerializedDataType`; sub-packages: `avro/` (Avro schema fingerprint codec) and `utils/` (type inference and caching) |
| `storage/` | `Flushable` interface |
| `tasks/` | All task-related data, messages, and storage — see breakdown below |
| `transport/` | Topic definitions, consumer/producer interfaces, batching utilities, logged decorators |
| `utils/` | `ClassUtil`, `IdGenerator`, `BatchUtil`, `JsonAble`, `merge.kt` |
| `workers/` | `WorkerName`, `WorkflowVersion`, `RetryPolicy` |
| `workflows/` | All workflow data, engine state/messages, executor contracts, tags — see breakdown below |

### tasks/ breakdown

| Sub-path | Content |
|----------|---------|
| `tasks/data/` | `TaskId`, `ServiceName`, `TaskAttemptId`, `TaskRetryIndex`, `TaskRetrySequence`, `TaskReturnValue`, `TaskMeta`, `TaskTag`, `DelegatedTaskData` |
| `tasks/executors/messages/` | `ServiceExecutorMessage`, `ServiceExecutorEnvelope`, `ServiceExecutorMessageType` |
| `tasks/events/messages/` | `ServiceExecutorEventMessage`, `ServiceEventEnvelope`, `ServiceEventMessageType` |
| `tasks/tags/messages/` | `ServiceTagMessage`, `ServiceTagEnvelope`, `TaskTagMessageType` |

### workflows/ breakdown

| Sub-path | Content |
|----------|---------|
| `workflows/data/channels/` | `ChannelName`, `ChannelType`, `ChannelFilter`, `ReceivingChannel`, `SignalData`, `SignalId` |
| `workflows/data/commands/` | `Command`, `CommandId`, `CommandName`, `CommandStatus`, `PastCommand` |
| `workflows/data/properties/` | `PropertyName`, `PropertyHash`, `PropertyValue` — content-addressed property store |
| `workflows/data/steps/` | `Step`, `NewStep`, `PastStep`, `StepStatus`, `StepHash`, `StepId`; `and()`/`or()` combinators |
| `workflows/data/timers/` | Timer-related data types |
| `workflows/data/workflowMethods/` | `WorkflowMethod`, `WorkflowMethodId`, `PositionInWorkflowMethod` |
| `workflows/data/workflowTasks/` | `WorkflowTaskParameters`, `WorkflowTaskReturnValue`, `WorkflowTaskIndex` |
| `workflows/data/workflows/` | `WorkflowId`, `WorkflowName`, `WorkflowTag`, `WorkflowMeta` |
| `workflows/engine/messages/` | `WorkflowMessage`, `WorkflowStateCmdMessage`, `WorkflowStateEngineMessage`, `WorkflowStateEventMessage`, three envelope types |
| `workflows/engine/state/` | `WorkflowState` (authoritative instance state), `Parser.kt` |
| `workflows/engine/storage/` | `WorkflowStateStorage` interface |
| `workflows/executors/` | `WorkflowDispatcher`, `WorkflowContext` |
| `workflows/tags/messages/` | `WorkflowTagEngineMessage`, `WorkflowTagEnvelope`, `WorkflowTagMessageType` |
| `workflows/tags/storage/` | Workflow-tag storage interface |

## For AI Agents

### Working In This Directory

**Adding a new message type**
1. Create the data class implementing `Message` (or the appropriate sealed subtype such as `WorkflowStateEngineMessage`).
2. Add a variant to the corresponding `*MessageType` enum.
3. Add it to the `*Envelope` union type and its `from()` / `message()` mapping.
4. Add an Avro `@AvroDefault` annotation for any field not present in older schema versions.
5. Add the new Avro schema file under `src/main/resources/schemas/` if required.

**Modifying `WorkflowState`**
- `WorkflowState` is persisted as Avro binary with schema fingerprinting. Any field addition requires `@AvroDefault(Avro.NULL)` (or another explicit default) to maintain backward compatibility with stored bytes from older versions.
- The class is a `@Serializable` `data class` — use Avro4k annotations (`@AvroName`, `@AvroNamespace`, `@AvroDefault`) rather than kotlinx.serialization annotations when renaming or defaulting fields.
- `AvroSerDe.readBinaryWithSchemaFingerprint` reads the stored schema from the byte prefix and migrates using Avro's schema evolution rules.

**Adding a new topic**
- Add a `data object` extending the appropriate sealed subclass (`ServiceTopic`, `WorkflowTopic`, or `Topic` directly) in `Topic.kt`.
- Update the `.withoutDelay` and `.forWorkflow` extension properties if the topic is a delay or workflow-task variant.
- Update `InfiniticConsumerFactory` and `InfiniticProducerFactory` interfaces in `transport/interfaces/`.

**Working with `SerializedData`**
- Supports multiple encoding strategies (`SerializedDataType`): JSON (Jackson), Kotlin serialization, and a null type.
- When adding new parameter/return-value types, verify that `inferJavaType` in `serDe/utils/` can resolve the type, and that both Java (`Jackson`) and Kotlin (`kotlinx.serialization`) paths handle it.

**Proxy handlers**
- `ProxyHandler` uses `ThreadLocal` state (`isInlined`, `isInvocationAsync`, `invocationHandler`) to capture the last-intercepted method call.
- `New*ProxyHandler` is used for dispatch; `Existing*ProxyHandler` for operations on already-running instances.
- Do not store captured invocation state across coroutine suspension points — the thread-local pattern is not coroutine-safe.

### Testing Requirements

- Run `./gradlew :infinitic-common:test` to execute unit tests.
- Run `./gradlew :infinitic-common:check` to include Spotless formatting checks.
- Schema evolution tests live in `src/test/` and use `AvroSerDe` round-trip assertions.
- Any new `@Serializable` class that appears in a stored envelope or state object must have a schema test that verifies backward compatibility with `AvroSerDe.readBinaryWithSchemaFingerprint`.

### Common Patterns

**Avro-annotated data class**
```kotlin
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class MyMessage(
  val id: SomeId,
  @AvroDefault(Avro.NULL) val optionalField: String? = null,
) : WorkflowStateEngineMessage
```

**Sending a message via `InfiniticProducer`**
```kotlin
// Inside a suspend context with an InfiniticProducer in scope
with(producer) {
  myMessage.sendTo(WorkflowStateEngineTopic)
  myMessage.sendTo(WorkflowStateTimerTopic, after = MillisDuration(5000))
}
```

**Deserializing `WorkflowState` from storage**
```kotlin
val state: WorkflowState = WorkflowState.fromByteArray(bytes)
val bytes: ByteArray = state.toByteArray()
```

**Using `SerializedData` for task return values**
```kotlin
val data = SerializedData.from(returnValue)
val recovered = data.deserialize(returnType, null)
```

## Dependencies

### Internal

None. This is the root module. No other `infinitic-*` module is imported here.

### External

| Library | Usage |
|---------|-------|
| `avro4k` (Avro4k) | Kotlin-first Avro serialization; `@AvroNamespace`, `@AvroDefault`, `@AvroName` annotations; `AvroSerDe` codec |
| `kotlinx.serialization` (json + core) | `@Serializable` on all message and state types; JSON encoding for debug/CloudEvents output |
| `Jackson` (databind + kotlin + jsr310) | Java-type serialization path inside `SerializedData`; config deserialization via Hoplite |
| `Hoplite` (core + yaml) | YAML/properties-based configuration loading |
| `jayway JsonPath` | JSON path evaluation on client API |
| `CloudEvents` (api + json) | CloudEvents envelope construction in `infinitic-cloudevents` (types defined here) |
| `Kotlin Coroutines` (core + jdk8) | `suspend` functions in producer/consumer interfaces; transport batching pipelines |
| `uuid-generator` | Fast UUID generation for `MessageId`, `TaskId`, `WorkflowId`, etc. |
| `Mockk` | Available at compile scope (used in some common test helpers) |

### testFixtures External

| Library | Usage |
|---------|-------|
| `EasyRandom` | Generates random instances of all message/data types for property-based tests |
| `Kotest` (junit5 + property) | Test runner and property-based test framework used across all modules |
| `Pulsar client + admin` | `PulsarContainer` test fixture using TestContainers |
| `TestContainers` | Base container support; exposed as `testFixturesApi` so consumer modules inherit it |
