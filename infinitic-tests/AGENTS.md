<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-tests

## Purpose
End-to-end integration test suite for the entire Infinitic system. Tests exercise the full stack (client → transport → workflow engine → task executor → storage) using in-memory transport. Covers all major workflow features: branching, channels, child workflows, deferred results, error handling, timers, tags, versioning, and more.

## Key Files

| File | Description |
|------|-------------|
| `src/test/kotlin/io/infinitic/scaffolds/` | Test scaffolding — shared test infrastructure, worker setup, service stubs |
| `src/test/kotlin/io/infinitic/utils/` | Test utilities and helper functions |

## Subdirectories (Test Categories)

| Directory | Purpose |
|-----------|---------|
| `src/test/kotlin/io/infinitic/tests/batches/` | Batch task dispatch and aggregation tests |
| `src/test/kotlin/io/infinitic/tests/branches/` | Parallel branch execution tests |
| `src/test/kotlin/io/infinitic/tests/cache/` | Storage caching behavior tests |
| `src/test/kotlin/io/infinitic/tests/channels/` | Signal channel send/receive tests |
| `src/test/kotlin/io/infinitic/tests/children/` | Child workflow dispatch and result propagation |
| `src/test/kotlin/io/infinitic/tests/context/` | `TaskContext` and `WorkflowContext` access tests |
| `src/test/kotlin/io/infinitic/tests/deferred/` | Deferred result `await()` and `getCompleted()` tests |
| `src/test/kotlin/io/infinitic/tests/delegation/` | Workflow delegation patterns |
| `src/test/kotlin/io/infinitic/tests/errors/` | Error propagation, retry, and failure handling tests |
| `src/test/kotlin/io/infinitic/tests/inline/` | Inline task execution tests |
| `src/test/kotlin/io/infinitic/tests/jsonView/` | JSON serialization view tests |
| `src/test/kotlin/io/infinitic/tests/properties/` | Workflow property access and mutation tests |
| `src/test/kotlin/io/infinitic/tests/syntax/` | Workflow syntax validation tests |
| `src/test/kotlin/io/infinitic/tests/tags/` | Workflow and task tag query/cancel tests |
| `src/test/kotlin/io/infinitic/tests/timeouts/` | Task and workflow timeout behavior tests |
| `src/test/kotlin/io/infinitic/tests/timers/` | Duration and instant timer tests |
| `src/test/kotlin/io/infinitic/tests/versioning/` | Workflow versioning and migration tests |

## For AI Agents

### Working In This Directory
- Tests use **in-memory transport** — no Docker or external services required
- All tests run a complete Infinitic worker in-process using `InfiniticWorker` with `InMemoryTransportConfig`
- Test workflows and services are defined in `scaffolds/` — add new ones there when needed
- Tests are grouped by feature area — add new tests to the appropriate subdirectory

### Testing Requirements
- `./gradlew :infinitic-tests:test`
- Fast — no external dependencies, runs entirely in-memory
- Add a test for any bug fix or new feature to prevent regression

### Common Patterns
- Each test class sets up a worker with specific workflow/service registrations
- Use `client.dispatch(MyWorkflow::class.java) { myMethod() }.await()` to run and assert
- Workflow test stubs in `scaffolds/` implement both the interface and the "test" behavior

## Dependencies

### Internal
- `infinitic-worker` — full worker for test setup
- `infinitic-client` — client API for dispatching
- `infinitic-common` — test fixtures from `testFixtures` source set
- `infinitic-transport-inMemory` — in-memory transport

### External
- Kotest — test framework and assertions
- Mockk — mocking for service stubs

<!-- MANUAL: -->
