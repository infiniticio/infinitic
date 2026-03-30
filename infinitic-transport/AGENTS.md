<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-transport

## Purpose
Defines the core transport configuration abstractions used across Infinitic. This module contains only configuration classes (not runtime interfaces, which live in `infinitic-common`) — it provides the sealed `TransportConfig` hierarchy and specific configs for Pulsar and in-memory transports, enabling YAML-based transport selection.

## Key Files

| File | Description |
|------|-------------|
| `src/main/kotlin/io/infinitic/transport/config/TransportConfig.kt` | Sealed class for transport configuration — discriminates between Pulsar and in-memory |
| `src/main/kotlin/io/infinitic/transport/config/TransportConfigInterface.kt` | Interface implemented by configs that contain a transport config |
| `src/main/kotlin/io/infinitic/transport/config/PulsarTransportConfig.kt` | Pulsar transport config wrapper — references `PulsarConfig` from `infinitic-transport-pulsar` |
| `src/main/kotlin/io/infinitic/transport/config/InMemoryTransportConfig.kt` | In-memory transport config (no connection parameters needed) |

## For AI Agents

### Working In This Directory
- This module is purely configuration — no runtime transport logic lives here
- Runtime transport interfaces (`InfiniticConsumer`, `InfiniticProducer`, etc.) are in `infinitic-common`
- Actual transport implementations are in `infinitic-transport-pulsar` and `infinitic-transport-inMemory`

### Testing Requirements
- `./gradlew :infinitic-transport:test`
- Minimal tests — primarily config deserialization tests

### Common Patterns
- `TransportConfig` is a sealed class: either `PulsarTransportConfig` or `InMemoryTransportConfig`
- Config loaded from YAML via Hoplite in worker/client config

## Dependencies

### Internal
- `infinitic-transport-pulsar` — referenced for `PulsarConfig`
- `infinitic-transport-inMemory` — referenced for in-memory config

### External
- kotlinx.serialization — config serialization

<!-- MANUAL: -->
