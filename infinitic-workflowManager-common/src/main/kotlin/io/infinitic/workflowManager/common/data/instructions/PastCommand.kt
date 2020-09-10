package io.infinitic.workflowManager.common.data.instructions

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskOutput
import io.infinitic.workflowManager.common.data.commands.CommandHash
import io.infinitic.workflowManager.common.data.commands.CommandSimpleName
import io.infinitic.workflowManager.common.data.commands.NewCommand
import io.infinitic.workflowManager.common.data.workflows.WorkflowIntegrityCheckMode
import io.infinitic.workflowManager.data.commands.CommandStatus

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TaskDispatched::class, name = "TASK_DISPATCHED")
//    JsonSubTypes.Type(value = ChildWorkflowDispatched::class, name = "CHILD_WORKFLOW_DISPATCHED"),
//    JsonSubTypes.Type(value = DelayWaited::class, name = "DELAY_WAITED"),
//    JsonSubTypes.Type(value = EventWaited::class, name = "EVENT_WAITED"),
//    JsonSubTypes.Type(value = InstantTaskRun::class, name = "INSTANT_TASK_RUN"),
//    JsonSubTypes.Type(value = WorkflowPaused::class, name = "WORKFLOW_PAUSED"),
//    JsonSubTypes.Type(value = WorkflowResumed::class, name = "WORKFLOW_RESUMED"),
//    JsonSubTypes.Type(value = WorkflowCompleted::class, name = "WORKFLOW_COMPLETED"),
//    JsonSubTypes.Type(value = WorkflowTerminated::class, name = "WORKFLOW_TERMINATED"),
//    JsonSubTypes.Type(value = EventSent::class, name = "EVENT_SENT")
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class PastCommand(
    open val commandPosition: Position,
    open val commandHash: CommandHash,
    open val commandSimpleName: CommandSimpleName,
    open val commandStatus: CommandStatus,
    open val commandOutput: Any?
) : PastInstruction(commandPosition) {
    override fun isSimilarTo(newCommand: NewCommand, mode: WorkflowIntegrityCheckMode): Boolean =
        newCommand.commandPosition == commandPosition && when (mode) {
            WorkflowIntegrityCheckMode.NONE -> true
            WorkflowIntegrityCheckMode.SIMPLE_NAME_ONLY -> newCommand.commandSimpleName == commandSimpleName
            WorkflowIntegrityCheckMode.ALL -> newCommand.commandHash == commandHash
        }
}

data class TaskDispatched(
    val taskId: TaskId,
    var taskOutput: TaskOutput?,
    override val commandPosition: Position,
    override val commandHash: CommandHash,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED
) : PastCommand(commandPosition, commandHash, commandSimpleName, commandStatus, taskOutput?.data)

// data class ChildWorkflowDispatched(
//    val childWorkflowId: WorkflowId,
//    var childWorkflowOutput: WorkflowMethodOutput?,
//    override val commandPosition: Position,
//    override val commandHash: CommandHash,
//    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED
// ) : PastCommand(commandPosition, commandHash, commandStatus)
//
// data class DelayWaited(
//    val delayId: DelayId,
//    override val commandPosition: Position,
//    override val commandHash: CommandHash,
//    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED
// ) : PastCommand(commandPosition, commandHash, commandStatus)
//
// data class EventWaited(
//    val eventId: EventId,
//    val eventName: EventName,
//    var eventData: EventData?,
//    override val commandPosition: Position,
//    override val commandHash: CommandHash,
//    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED
// ) : PastCommand(commandPosition, commandHash, commandStatus)

/**
 * InstantTask have already been processed by the Decider
 */
// data class InstantTaskRun(
//    var taskOutput: TaskOutput,
//    val commandPosition: Position,
//    override override val commandHash: CommandHash,
//    override val commandStatus: CommandStatus
// ) : PastCommand(commandPosition, commandHash, commandStatus)

/**
 * EngineCommand are processed right away by the Engine
 */
// sealed class PastEngineCommand(
//    override commandPosition: Position,
//    override val commandHash: CommandHash,
//    override val commandStatus: CommandStatus
// ) : PastCommand(commandPosition, commandHash, commandStatus)
//
// data class WorkflowPaused(
//    override val commandPosition: Position,
//    override val commandHash: CommandHash,
//    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED
// ) : PastEngineCommand(commandPosition, commandHash, commandStatus)
//
// data class WorkflowResumed(
//    override val commandPosition: Position,
//    override val commandHash: CommandHash,
//    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED
// ) : PastEngineCommand(commandPosition, commandHash, commandStatus)
//
// data class WorkflowCompleted(
//    override val commandPosition: Position,
//    val workflowOutput: WorkflowMethodOutput?,
//    override val commandHash: CommandHash,
//    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED
// ) : PastEngineCommand(commandPosition, commandHash, commandStatus)
//
// data class WorkflowTerminated(
//    override val commandPosition: Position,
//    override val commandHash: CommandHash,
//    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED
// ) : PastEngineCommand(commandPosition, commandHash, commandStatus)
//
// data class EventSent(
//    val eventName: EventName,
//    var eventData: EventData?,
//    override val commandPosition: Position,
//    override val commandHash: CommandHash,
//    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED
// ) : PastEngineCommand(commandPosition, commandHash, commandStatus)
