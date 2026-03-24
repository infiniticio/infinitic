<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-workflow-engine

## Purpose
The core workflow state engine that orchestrates workflow execution. Processes incoming messages (task completions, child workflow events, signals, timers) and manages workflow state transitions. Implements event-sourcing via `PastCommand` tracking in `WorkflowState`. This is the most complex module in the system.

## Key Files

| File | Description |
|------|-------------|
| `src/main/kotlin/io/infinitic/workflows/engine/WorkflowStateEngine.kt` | Main entry point — routes incoming messages to appropriate handlers |
| `src/main/kotlin/io/infinitic/workflows/engine/WorkflowStateCmdHandler.kt` | Handles command messages (dispatch workflow, dispatch method, cancel, retry) |
| `src/main/kotlin/io/infinitic/workflows/engine/WorkflowStateEventHandler.kt` | Handles event messages (task completed/failed, child workflow events) |
| `src/main/kotlin/io/infinitic/workflows/engine/WorkflowStateTimerHandler.kt` | Handles timer completion messages |
| `src/main/kotlin/io/infinitic/workflows/engine/storage/BinaryWorkflowStateStorage.kt` | Serializes/deserializes `WorkflowState` to/from binary storage |
| `src/main/kotlin/io/infinitic/workflows/engine/storage/LoggedWorkflowStateStorage.kt` | Logging decorator for workflow state storage |
| `src/main/kotlin/io/infinitic/workflows/engine/helpers/CommandTerminated.kt` | Helper to mark a past command as terminated and check if steps can proceed |
| `src/main/kotlin/io/infinitic/workflows/engine/helpers/DispatchWorkflowTask.kt` | Helper to dispatch the special workflow task for re-executing workflow method code |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `src/main/kotlin/io/infinitic/workflows/engine/handlers/` | Individual message handlers (one file per message type) |
| `src/main/kotlin/io/infinitic/workflows/engine/helpers/` | Shared helper functions for state manipulation |
| `src/main/kotlin/io/infinitic/workflows/engine/storage/` | WorkflowState storage wrapper and logging decorator |

## Handlers

Each file in `handlers/` processes one specific message type:

| Handler | Triggered When |
|---------|---------------|
| `dispatchWorkflow.kt` | A new workflow is dispatched |
| `dispatchMethod.kt` | A new method is dispatched on an existing workflow |
| `taskCompleted.kt` | A service task completes successfully |
| `taskFailed.kt` | A service task fails |
| `taskCanceled.kt` | A service task is canceled |
| `taskTimedOut.kt` | A service task times out |
| `workflowTaskCompleted.kt` | The workflow task (replaying method code) completes |
| `workflowTaskFailed.kt` | The workflow task fails |
| `childMethodCompleted.kt` | A child workflow method completes |
| `childMethodFailed.kt` | A child workflow method fails |
| `childMethodCanceled.kt` | A child workflow method is canceled |
| `childMethodTimedOut.kt` | A child workflow method times out |
| `childMethodUnknown.kt` | A child workflow is unknown |
| `cancelWorkflow.kt` | A workflow cancellation is requested |
| `sendSignal.kt` | A signal is sent to a workflow channel |
| `completeTimer.kt` | A timer completes |
| `timerCompleted.kt` | Timer completion event |
| `retryTasks.kt` | Manual task retry is requested |
| `retryWorkflowTask.kt` | Manual workflow task retry is requested |
| `waitWorkflow.kt` | Client waits for workflow result |

## For AI Agents

### Working In This Directory
- `WorkflowState` (defined in `infinitic-common`) is the central data structure — all handlers read and mutate it
- The engine is purely functional: given a state + message, it produces a new state + output messages
- `PastCommand` tracking in state enables event-sourcing and deterministic replay
- Always run `spotlessApply` — this module has complex formatting

### Testing Requirements
- `./gradlew :infinitic-workflow-engine:test`
- Tests use mock transport and storage via Mockk

### Common Patterns
- Each handler follows: load state → validate → mutate state → emit messages → save state
- `DispatchWorkflowTask` helper triggers workflow code re-execution after any state change
- `CommandTerminated` helper advances step resolution when a past command completes

## Dependencies

### Internal
- `infinitic-common` — `WorkflowState`, all message types, storage interfaces, transport interfaces

### External
- Kotlin Coroutines — async message processing
- Avro4k — state serialization

<!-- MANUAL: -->
