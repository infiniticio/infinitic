<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-workflow-task

## Purpose

Executes the special "workflow task" that runs workflow method code deterministically. Given a `WorkflowTaskParameters` snapshot (containing the full command/step history), it replays the workflow method up to the current position, discovers new commands, and returns a `WorkflowTaskReturnValue` to the engine. This is the bridge between the task executor and the workflow engine.

## Key Files

| File | Description |
|------|-------------|
| `workflowTask/WorkflowTaskImpl.kt` | Implements `WorkflowTask.handle()`; sets up the dispatcher on the workflow instance, restores properties, invokes the method via reflection, and returns new commands/step/properties |
| `workflowTask/WorkflowDispatcherImpl.kt` | Implements `WorkflowDispatcher`; intercepts every workflow operation (task dispatch, child workflow, timer, signal send/receive, inline task, await) and either replays from history or records as a new command |
| `workflowTask/WorkflowStepException.kt` | Internal control-flow exceptions: `NewStepException` (execution reached an unresolved step — stop here) and `KnownStepException` (execution reached a resolved but already-recorded step — stop here) |

## For AI Agents

### Working In This Directory

- The replay mechanism is the core invariant: every call to `dispatchCommand` or `await` first checks `workflowMethod.pastCommands` / `pastSteps` at the current `positionInMethod`. If history matches, the past result is returned; if there is no history entry, the command is new.
- `WorkflowStepException` is not an error — it is thrown intentionally to halt execution when the method reaches a point that cannot be completed yet. `WorkflowTaskImpl` catches it and returns `methodReturnValue = null`.
- Command IDs are **deterministic**: derived from `workflowTaskInstant`, `workflowId`, `workflowMethodId`, `workflowVersion`, and `positionInMethod`. This guarantees idempotency if a workflow task is processed twice. Do not change this derivation without understanding the downstream impact on the engine.
- `WorkflowCheckMode` controls how strictly history is validated. In `SAFE` mode a mismatch throws `WorkflowChangedException`; this surfaces non-deterministic workflow code to the developer.
- `positionInMethod` is a monotonically incrementing cursor. Every command and every `await` call advances it. Branches (parallel steps) use a sub-position scheme encoded in `PositionInWorkflowMethod`.
- Properties are managed by hash: `WorkflowDispatcherImpl` receives a `setProperties` lambda from `WorkflowTaskImpl` that maps `PropertyName → PropertyHash` back to actual values from the engine-supplied `workflowPropertiesHashValue` map.

### Testing Requirements

```bash
./gradlew :infinitic-workflow-task:test
```

- Tests use Kotest.
- `WorkflowTaskImpl` and `WorkflowDispatcherImpl` can be unit-tested by constructing a `WorkflowTaskParameters` with a synthetic history and asserting on the returned `WorkflowTaskReturnValue`.

### Common Patterns

- Replay pattern in `dispatchCommand`: call `getPastCommandAtCurrentPosition()` — if non-null and matching, return it; if non-null and mismatched, throw `WorkflowChangedException`; if null, record as new command.
- Replay pattern in `await`: call `getSimilarPastStep()` — if null, evaluate step status from command history; if found and still `Waiting`, throw `KnownStepException`; if found and resolved, restore properties and return the value.
- `inline { }` tasks are recorded immediately as `CommandStatus.Completed` in the same workflow task execution — they never produce async commands.

## Dependencies

### Internal

- `infinitic-common` — `WorkflowTask` interface, `WorkflowTaskParameters`, `WorkflowTaskReturnValue`, all `PastCommand` subtypes, `Step`/`StepStatus`, `WorkflowDispatcher`, proxy handlers

### External

- `kotlin-reflect` — method invocation and return-type inspection via `java.lang.reflect`
- `kotlinx-coroutines-core` — coroutine support
- `jayway-jsonpath` — `ChannelFilter` evaluation for signal receive filtering
- `slf4j-api` — logging
