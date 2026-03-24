<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-task-executor

## Purpose

Executes service tasks and workflow tasks. `TaskExecutor` receives `ExecuteTask` messages,
invokes user-defined service methods (or `WorkflowTaskImpl` for workflow tasks) via reflection,
applies timeout and retry policies, and publishes lifecycle events
(`TaskStartedEvent`, `TaskCompletedEvent`, `TaskFailedEvent`, `TaskRetriedEvent`).
`TaskEventHandler` processes those events and fans out results to the requesting client or
workflow engine. `TaskRetryHandler` re-routes retry messages to the correct topic after
their delay elapses.

## Key Files

| File | Description |
|------|-------------|
| `TaskExecutor.kt` | Core executor. Handles single and batch `ExecuteTask` messages. Resolves service/workflow instances from `ExecutorRegistryInterface`, applies `WithTimeout`/`WithRetry` policies, dispatches retry or failure events. Default workflow task timeout is 60 s. |
| `TaskEventHandler.kt` | Processes `ServiceExecutorEventMessage` events. On `TaskCompletedEvent`: forwards result to the requesting client or workflow engine, removes tags, and — for workflow tasks — dispatches all new commands produced by the workflow method. On `TaskFailedEvent`: notifies the requesting client or workflow engine. |
| `TaskRetryHandler.kt` | Receives messages from the retry topic and re-sends them to `WorkflowExecutorTopic` or `ServiceExecutorTopic` depending on whether the task is a workflow task. |
| `task/TaskRunner.kt` | Runs a task lambda in a thread-pool `ExecutorService` with a hard timeout using `Future.get(timeout)`. Supports a configurable grace period after timeout expiry before cancelling the future. Uses `TimeoutContext` to notify the task of impending cancellation. |
| `task/TaskContextImpl.kt` | Concrete `TaskContext` implementation populated at execution time. Exposes worker name, service name, task id, retry index, workflow context, tags, meta, and the embedded `InfiniticClientInterface`. |
| `task/TaskCommand.kt` | Data class representing a command extracted from a task invocation. |
| `events/dispatchTaskCmd.kt` | Dispatches a `DispatchTaskPastCommand` to the service executor topic. |
| `events/dispatchRemoteWorkflowCmd.kt` | Dispatches a `DispatchNewWorkflowPastCommand` after a workflow task completes. |
| `events/dispatchRemoteMethodCmd.kt` | Dispatches a `DispatchNewMethodPastCommand` to an existing workflow. |
| `events/dispatchRemoteSignalCmd.kt` | Dispatches a `SendSignalPastCommand` to a workflow channel. |
| `events/dispatchDurationTimerCmd.kt` | Dispatches a `StartDurationTimerPastCommand` to the timer topic. |
| `events/dispatchInstantTimerCmd.kt` | Dispatches a `StartInstantTimerPastCommand` to the timer topic. |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `task/` | Task execution primitives: thread-pool runner, context implementation, command data. |
| `events/` | Post-execution command dispatch functions for each `PastCommand` type produced by a workflow task. |

## For AI Agents

### Working In This Directory

- `TaskExecutor` uses `ExecutorRegistryInterface` (implemented by `ExecutorRegistry` in
  `infinitic-worker`) to obtain service/workflow instances. It does not instantiate
  classes itself.
- Timeout resolution order: registry config > `@Timeout` annotation or `WithTimeout`
  interface on the method > default. For workflow tasks the default is 60 s; for service
  tasks there is no default timeout.
- Retry resolution order: registry config > `@Retry` annotation or `WithRetry` interface
  on the method > default (no retries for both service and workflow tasks).
- Batch processing: if a service method has a corresponding `@Batch`-annotated method,
  `batchProcess` groups messages and invokes the batch method. The batch method must return
  `Map<String, R>` keyed by task id string. Missing or extra keys cause a `TaskFailedEvent`.
- Delegated tasks (`@Delegated` annotation): the return value is discarded; a
  `SetDelegatedTaskData` message is sent to `ServiceTagEngineTopic` so the result can be
  submitted later via `InfiniticClient.completeDelegatedTaskAsync`.
- Workflow tasks are identified by `ServiceName.isWorkflowTask()`. They always run
  individually (never batched) and execute `WorkflowTaskImpl.handle`.
- After a workflow task completes, `TaskEventHandler.completeWorkflowTask` dispatches every
  new command in `WorkflowTaskReturnValue.newCommands`. This dispatch happens *after* sending
  the completion to the workflow engine to prevent race conditions.
- `TaskRunner` runs the actual invocation on a thread from the provided `ExecutorService`,
  not on a coroutine thread, to allow genuine thread interruption on timeout.

### Testing Requirements

- The module has no direct test dependencies on a transport; use `infinitic-transport-inMemory`
  from the worker or test modules.
- Run module tests: `./gradlew :infinitic-task-executor:test`
- When adding a new `PastCommand` type, add a corresponding dispatch function in `events/` and
  handle it in `TaskEventHandler.completeWorkflowTask`.

### Common Patterns

- Service task lifecycle: `sendTaskStarted` → `parse` → `getTimeoutMillis` / `getGraceMillis`
  → `executeWithTimeout` → `sendTaskCompleted` or `retryOrSendTaskFailed`.
- Batch task lifecycle: same as above but all steps operate on `List<ExecuteTask>` and
  produce/consume `BatchData`.
- Accessing task context from user code: `Task.context` (reads the `ThreadLocal` set by
  `TaskExecutor` before invocation).

## Dependencies

### Internal

| Module | Role |
|--------|------|
| `infinitic-common` | `ExecuteTask` message, event messages, transport topics, serialization |
| `infinitic-workflow-task` | `WorkflowTaskImpl` and `WorkflowTaskParameters` for workflow task execution |

### External

| Library | Role |
|---------|------|
| `kotlin-reflect` | Method resolution by name and parameter types |
| `kotlinx-coroutines-core` | `coroutineScope`, `async`, `launch` for parallel dispatch |
