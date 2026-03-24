<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-task-tag

## Purpose

Manages tag-to-task-id mappings and delegated task data for services. `TaskTagEngine`
processes `ServiceTagMessage` events: it stores and retrieves task ids indexed by tag,
persists `DelegatedTaskData` for tasks marked with `@Delegated`, and — when a delegated
task is completed externally — forwards the result to the original requesting client or
workflow engine.

## Key Files

| File | Description |
|------|-------------|
| `TaskTagEngine.kt` | Message handler for `ServiceTagMessage`. Routes `AddTaskIdToTag`, `RemoveTaskIdFromTag`, `SetDelegatedTaskData`, `CompleteDelegatedTask`, and `GetTaskIdsByTag` to the appropriate storage or producer calls. |
| `storage/BinaryTaskTagStorage.kt` | `TaskTagStorage` implementation. Uses a `KeySetStorage` for tag→taskId sets and a `KeyValueStorage` for delegated task data. Storage keys follow the pattern `task:{serviceName}|tag:{tag}|setIds` and `taskId:{taskId}|delegatedTaskData`. Data is serialised as binary. |
| `storage/LoggedTaskTagStorage.kt` | Decorator over `TaskTagStorage` that adds trace/debug logging around every storage call. Used by `InfiniticWorker` to wrap `BinaryTaskTagStorage`. |

## For AI Agents

### Working In This Directory

- `TaskTagEngine` is stateless; all persistent state lives in `TaskTagStorage`.
- `BinaryTaskTagStorage` wraps raw `KeyValueStorage` and `KeySetStorage` with
  `WrappedKeyValueStorage` / `WrappedKeySetStorage`, which convert storage exceptions into
  typed exceptions.
- Delegated task flow: when `TaskEventHandler` detects a completed delegated task, it sends
  `SetDelegatedTaskData` to `ServiceTagEngineTopic`. When a user later calls
  `InfiniticClient.completeDelegatedTaskAsync`, a `CompleteDelegatedTask` message arrives
  here; `TaskTagEngine` looks up the stored data and routes the result to the waiting client
  (`ClientTopic`) and/or workflow engine (`WorkflowStateEngineTopic`), then deletes the stored
  data.
- `CancelTaskByTag` is not yet implemented (marked `TODO()`).
- `RetryTaskByTag` is explicitly forbidden (`thisShouldNotHappen()`).
- The `flush()` method on storage implementations is for tests only (`@TestOnly`).

### Testing Requirements

- Storage can be tested in isolation using the in-memory implementations from
  `infinitic-storage`.
- Run module tests: `./gradlew :infinitic-task-tag:test`
- When adding a new `ServiceTagMessage` subtype, handle it in the `when` block in
  `TaskTagEngine.process`.

### Common Patterns

- Tag lookup flow: client sends `GetTaskIdsByTag` → engine calls `storage.getTaskIds` →
  engine sends `TaskIdsByTag` back to `ClientTopic`.
- Tag registration flow: executor sends `AddTaskIdToTag` on task dispatch → engine calls
  `storage.addTaskId`. Removal happens on task completion via `RemoveTaskIdFromTag`.
- Storage key format: `task:{serviceName}|tag:{tag}|setIds` for the id set;
  `taskId:{taskId}|delegatedTaskData` for delegated task data.

## Dependencies

### Internal

| Module | Role |
|--------|------|
| `infinitic-common` | `ServiceTagMessage` types, `TaskTagStorage` interface, transport topics |
| `infinitic-storage` | `KeyValueStorage`, `KeySetStorage`, and their wrapped variants |
| `infinitic-cache` | Caching layer (transitive via `infinitic-storage`) |

### External

| Library | Role |
|---------|------|
| `kotlinx-coroutines-core` | `coroutineScope` for parallel sends in `TaskTagEngine` |
| `kotlinx-coroutines-jdk8` | JDK8 coroutine bridge |
