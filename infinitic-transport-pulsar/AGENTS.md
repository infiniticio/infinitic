<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-transport-pulsar

## Purpose
Apache Pulsar transport implementation — the production message transport for Infinitic. Manages Pulsar topics, consumer subscriptions, producer creation, Avro schema registration, and message routing. Implements the transport interfaces defined in `infinitic-common` against the Pulsar client library.

## Key Files

| File | Description |
|------|-------------|
| `src/main/kotlin/io/infinitic/pulsar/PulsarInfiniticConsumerFactory.kt` | Factory that creates Pulsar consumers for each topic/subscription |
| `src/main/kotlin/io/infinitic/pulsar/PulsarInfiniticProducer.kt` | Pulsar producer that serializes and sends Infinitic messages |
| `src/main/kotlin/io/infinitic/pulsar/PulsarInfiniticProducerFactory.kt` | Factory that creates Pulsar producers |
| `src/main/kotlin/io/infinitic/pulsar/PulsarInfiniticResources.kt` | Topic and namespace lifecycle management via Pulsar Admin API |
| `src/main/kotlin/io/infinitic/pulsar/admin/InfiniticPulsarAdmin.kt` | Wrapper around Pulsar Admin client for namespace/topic operations |
| `src/main/kotlin/io/infinitic/pulsar/admin/AdminConfigInterface.kt` | Admin configuration interface |
| `src/main/kotlin/io/infinitic/pulsar/client/InfiniticPulsarClient.kt` | Wrapper around Pulsar client for producer/consumer creation |
| `src/main/kotlin/io/infinitic/pulsar/client/ClientConfigInterface.kt` | Client configuration interface |
| `src/main/kotlin/io/infinitic/pulsar/config/PulsarConfig.kt` | Top-level Pulsar configuration (broker URL, tenant, namespace) |
| `src/main/kotlin/io/infinitic/pulsar/config/PulsarClientConfig.kt` | Pulsar client-level settings (connection timeout, TLS, auth) |
| `src/main/kotlin/io/infinitic/pulsar/config/PulsarConsumerConfig.kt` | Consumer settings (receive queue size, dead letter policy) |
| `src/main/kotlin/io/infinitic/pulsar/config/PulsarProducerConfig.kt` | Producer settings (batching, compression, send timeout) |
| `src/main/kotlin/io/infinitic/pulsar/config/auth/` | Authentication configs: Token, OAuth2, SASL, Athenz |
| `src/main/kotlin/io/infinitic/pulsar/config/policies/PoliciesConfig.kt` | Pulsar namespace policy configuration |
| `src/main/kotlin/io/infinitic/pulsar/consumers/PulsarTransportConsumer.kt` | Coroutine-based Pulsar message consumer |
| `src/main/kotlin/io/infinitic/pulsar/consumers/PulsarTransportMessage.kt` | Wraps a Pulsar message for transport delivery |
| `src/main/kotlin/io/infinitic/pulsar/resources/PulsarResources.kt` | Topic naming and resource management logic |
| `src/main/kotlin/io/infinitic/pulsar/resources/Topics.kt` | Topic naming conventions and topic type definitions |
| `src/main/kotlin/io/infinitic/pulsar/resources/PulsarSubscription.kt` | Subscription type definitions |
| `src/main/kotlin/io/infinitic/pulsar/schemas/KSchemaReader.kt` | Avro schema reader for Pulsar schema registry |
| `src/main/kotlin/io/infinitic/pulsar/schemas/KSchemaWriter.kt` | Avro schema writer for Pulsar schema registry |
| `src/main/kotlin/io/infinitic/pulsar/schemas/schemaDefinition.kt` | Schema registration and lookup utilities |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `src/main/kotlin/io/infinitic/pulsar/admin/` | Pulsar Admin API wrapper |
| `src/main/kotlin/io/infinitic/pulsar/client/` | Pulsar client wrapper |
| `src/main/kotlin/io/infinitic/pulsar/config/` | All Pulsar configuration classes |
| `src/main/kotlin/io/infinitic/pulsar/config/auth/` | Authentication provider configs |
| `src/main/kotlin/io/infinitic/pulsar/config/policies/` | Namespace policy config |
| `src/main/kotlin/io/infinitic/pulsar/consumers/` | Consumer implementation |
| `src/main/kotlin/io/infinitic/pulsar/resources/` | Topic and resource management |
| `src/main/kotlin/io/infinitic/pulsar/schemas/` | Avro schema integration |

## For AI Agents

### Working In This Directory
- Requires a running Pulsar broker (see `pulsar/` directory for local Docker setup)
- Topics follow a naming convention defined in `Topics.kt` — do not hardcode topic names
- Schema evolution is handled via Avro4k — be careful when modifying serialized message classes in `infinitic-common`
- Authentication is configured via `PulsarClientConfig` auth sub-config

### Testing Requirements
- `./gradlew :infinitic-transport-pulsar:test`
- Integration tests require Pulsar (TestContainers or local `docker compose up`)

### Common Patterns
- Consumers use `Exclusive` or `Shared` subscriptions depending on the topic role
- Messages are serialized using Avro4k schema before being sent to Pulsar
- Dead letter topics are configured for failed message handling

## Dependencies

### Internal
- `infinitic-common` — transport interfaces and message definitions

### External
- Apache Pulsar Java Client — producer/consumer API
- Apache Pulsar Admin Client — namespace/topic management
- Avro4k — Kotlin Avro serialization for Pulsar schema registry
- Kotlin Coroutines — async consumer processing

<!-- MANUAL: -->
