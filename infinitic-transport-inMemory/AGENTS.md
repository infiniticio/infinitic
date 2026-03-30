<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-transport-inMemory

## Purpose
In-memory transport implementation for unit and integration testing without external dependencies. Implements the transport interfaces from `infinitic-common` using Kotlin coroutine channels. Enables running the complete Infinitic stack (worker, client, workflow engine, task executors) entirely in-process.

## Key Files

| File | Description |
|------|-------------|
| `src/main/kotlin/io/infinitic/inMemory/InMemoryConsumerFactory.kt` | Factory that creates in-memory consumers for each topic |
| `src/main/kotlin/io/infinitic/inMemory/InMemoryInfiniticProducer.kt` | In-memory producer that sends messages directly to channels |
| `src/main/kotlin/io/infinitic/inMemory/InMemoryInfiniticProducerFactory.kt` | Factory that creates in-memory producers |
| `src/main/kotlin/io/infinitic/inMemory/InMemoryInfiniticResources.kt` | Resource manager — topic creation and lifecycle for in-memory transport |
| `src/main/kotlin/io/infinitic/inMemory/channels/InMemoryChannels.kt` | Coroutine channel registry — maps topics to `Channel<Message>` instances |
| `src/main/kotlin/io/infinitic/inMemory/consumers/InMemoryConsumer.kt` | Consumer that reads from a coroutine channel |
| `src/main/kotlin/io/infinitic/inMemory/consumers/InMemoryTransportMessage.kt` | Transport message wrapper for in-memory delivery |

## For AI Agents

### Working In This Directory
- No external services required — all communication is via Kotlin coroutine channels
- Used by `infinitic-tests` for integration tests (fast, no Docker needed)
- Does not support persistence — all state is lost when the process exits

### Testing Requirements
- `./gradlew :infinitic-transport-inMemory:test`

### Common Patterns
- `InMemoryChannels` holds one `Channel` per logical topic
- Message acknowledgment is a no-op (in-memory delivery is reliable)
- Used in tests via `InMemoryTransportConfig` in the worker/client YAML config

## Dependencies

### Internal
- `infinitic-common` — transport interfaces (`InfiniticConsumer`, `InfiniticProducer`, etc.)

### External
- Kotlin Coroutines — channels for message passing

<!-- MANUAL: -->
