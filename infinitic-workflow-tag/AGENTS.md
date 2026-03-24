<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-workflow-tag

## Purpose

Manages workflow tags: string labels attached to workflow instances. Stores `WorkflowTag → Set<WorkflowId>` mappings in a key-set store, and routes tag-scoped commands (cancel, signal, retry, dispatch) to every matching workflow instance. Also handles the `customId` feature, which uses a tag to enforce a singleton workflow per custom identifier.

## Key Files

| File | Description |
|------|-------------|
| `tag/WorkflowTagEngine.kt` | Processes all `WorkflowTagEngineMessage` types; batches by `(WorkflowTag, WorkflowName)`, resolves workflow IDs from storage, and fans out per-workflow commands to `WorkflowStateCmdTopic` |
| `tag/storage/BinaryWorkflowTagStorage.kt` | `WorkflowTagStorage` implementation using `KeySetStorage`; key format is `workflow:<name>\|tag:<tag>\|setIds`; stores workflow IDs as UTF-8 bytes |
| `tag/storage/BufferedWorkflowTagStorage.kt` | In-memory buffer used during batch processing; accumulates adds/removes and flushes them in a single `updateWorkflowIds` call at the end of the batch |
| `tag/storage/LoggedWorkflowTagStorage.kt` | Decorator that adds structured logging around every storage call |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `tag/storage/` | `WorkflowTagStorage` implementations |

## For AI Agents

### Working In This Directory

- `WorkflowTagEngine` is the single entry point. It has two public methods: `process` (single message) and `batchProcess` (list of messages).
- In batch mode, messages are grouped by `(WorkflowTag, WorkflowName)`. Workflow IDs for tags that need them are fetched in **one** bulk `getWorkflowIds` call before processing begins. Each tag group then uses a `BufferedWorkflowTagStorage` (pre-loaded with its ID set) to avoid repeated storage reads.
- `DispatchWorkflowByCustomId` implements singleton semantics: if a workflow with that tag already exists (exactly one ID), it skips dispatch and optionally registers the caller as a waiter; if none exists, it dispatches the workflow and adds the tag.
- `AddTagToWorkflow` / `RemoveTagFromWorkflow` are the only messages that do not need the existing ID set — they are filtered out of the bulk fetch.
- All fan-out commands are sent to `WorkflowStateCmdTopic`, not `WorkflowStateEngineTopic`. This routes through `WorkflowStateCmdHandler` for proper pre-processing.

### Testing Requirements

```bash
./gradlew :infinitic-workflow-tag:test
```

- Tests use Kotest and Mockk.
- Storage is tested via in-memory `KeySetStorage` from `infinitic-storage` test fixtures.

### Common Patterns

- Per-tag fan-out: `ids.forEach { workflowId -> launch { /* build per-workflow message */ sendTo(WorkflowStateCmdTopic) } }`.
- Always skip the sending workflow itself when fanning out signals or cancellations (`workflowId != message.requester.workflowId`) to prevent double-application.
- `BufferedWorkflowTagStorage` tracks adds and removes separately; the engine flushes them via `_storage.updateWorkflowIds(add = ..., remove = ...)` after all messages for a batch are processed.

## Dependencies

### Internal

- `infinitic-common` — all tag message types, `WorkflowTagStorage` interface, `WorkflowId`/`WorkflowTag`/`WorkflowName` data types
- `infinitic-cache` — (via `api` dependency) caching layer
- `infinitic-storage` — `KeySetStorage` abstraction backing `BinaryWorkflowTagStorage`

### External

- `kotlinx-coroutines-core` — parallel fan-out within `batchProcess`
- `kotlinx-coroutines-jdk8` — JDK8 coroutine extensions
