<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic

## Purpose
Infinitic is a workflow orchestration framework built on Kotlin that enables reliable, event-driven distributed architectures. It provides durable workflow execution, task dispatching, and state management on top of Apache Pulsar (primary) or in-memory transport (testing). The project uses a multi-module Gradle structure targeting Java 17.

## Key Files

| File | Description |
|------|-------------|
| `settings.gradle.kts` | Declares all subproject modules included in the build |
| `build.gradle.kts` | Root build configuration with shared settings |
| `publish.gradle.kts` | Maven Central publishing configuration |
| `gradle.properties` | Gradle JVM args and project-wide properties |
| `CLAUDE.md` | AI coding assistant instructions (build commands, style, architecture) |
| `Makefile` | Convenience targets including `addlicense` |
| `run_addlicence.sh` | Script to add Commons Clause license headers to all source files |
| `.editorconfig` | Editor config: 2-space indent, 100-char line length (ktfmt rules) |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `buildSrc/` | Gradle build logic: version constants, dependency catalog, plugin config (see `buildSrc/AGENTS.md`) |
| `infinitic-common/` | Shared data models, messages, serialization, and transport/storage interfaces (see `infinitic-common/AGENTS.md`) |
| `infinitic-cache/` | Caffeine-based caching layer (see `infinitic-cache/AGENTS.md`) |
| `infinitic-client/` | Client API for dispatching workflows and tasks (see `infinitic-client/AGENTS.md`) |
| `infinitic-cloudevents/` | CloudEvents integration for event interoperability (see `infinitic-cloudevents/AGENTS.md`) |
| `infinitic-dashboard/` | Kweb-based web dashboard for monitoring (see `infinitic-dashboard/AGENTS.md`) |
| `infinitic-storage/` | Key-value storage implementations: Redis, PostgreSQL, MySQL, in-memory (see `infinitic-storage/AGENTS.md`) |
| `infinitic-task-executor/` | Executes service tasks — where business logic runs (see `infinitic-task-executor/AGENTS.md`) |
| `infinitic-task-tag/` | Task tagging for querying and organizing tasks (see `infinitic-task-tag/AGENTS.md`) |
| `infinitic-tests/` | Integration test suite with TestContainers (see `infinitic-tests/AGENTS.md`) |
| `infinitic-transport/` | Core transport interfaces and abstractions (see `infinitic-transport/AGENTS.md`) |
| `infinitic-transport-inMemory/` | In-memory transport implementation for testing (see `infinitic-transport-inMemory/AGENTS.md`) |
| `infinitic-transport-pulsar/` | Apache Pulsar transport implementation (see `infinitic-transport-pulsar/AGENTS.md`) |
| `infinitic-utils/` | Shared utilities used across modules (see `infinitic-utils/AGENTS.md`) |
| `infinitic-worker/` | Worker runtime that runs workflow and task executors (see `infinitic-worker/AGENTS.md`) |
| `infinitic-workflow-engine/` | Workflow state engine: orchestrates execution and state transitions (see `infinitic-workflow-engine/AGENTS.md`) |
| `infinitic-workflow-tag/` | Workflow tagging for querying and organizing workflows (see `infinitic-workflow-tag/AGENTS.md`) |
| `infinitic-workflow-task/` | Handles workflow task execution within the workflow engine (see `infinitic-workflow-task/AGENTS.md`) |
| `pulsar/` | Docker Compose and data files for local Pulsar development |

## For AI Agents

### Working In This Directory
- All source files must include the Commons Clause license header — run `make addlicense` after adding new files
- Always run `./gradlew spotlessApply` before committing
- Version is in `buildSrc/src/main/kotlin/Ci.kt` — update `BASE` for releases
- Use `RELEASE=true ./gradlew publish --rerun-tasks` for publishing

### Testing Requirements
- `./gradlew test` — runs all module tests
- `./gradlew :module-name:test` — tests a specific module
- Integration tests in `infinitic-tests` require Docker (TestContainers)

### Common Patterns
- All inter-component communication uses `Message` implementations from `infinitic-common`
- Kotlin context receivers enabled (`-Xcontext-receivers`)
- `@OptIn(ExperimentalStdlibApi::class)` applied globally
- JVM default methods: `-Xjvm-default=all`
- 2-space indentation, 100-char line length (ktfmt enforced)

## Dependencies

### External
- Kotlin 1.9+ — primary language
- Apache Pulsar — message transport
- kotlinx.serialization + Avro4k — serialization
- Kotest + Mockk + TestContainers — testing
- Caffeine — caching
- Kweb + Tailwind CSS — dashboard UI
- Hoplite — configuration loading
- Redis, PostgreSQL, MySQL — storage backends

<!-- MANUAL: Any manually added notes below this line are preserved on regeneration -->
