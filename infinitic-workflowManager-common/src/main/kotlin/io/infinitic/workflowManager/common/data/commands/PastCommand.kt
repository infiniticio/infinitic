package io.infinitic.workflowManager.common.data.commands

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.common.data.DateTime
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskOutput
import io.infinitic.workflowManager.common.data.DelayId
import io.infinitic.workflowManager.common.data.events.EventData
import io.infinitic.workflowManager.common.data.events.EventId
import io.infinitic.workflowManager.common.data.events.EventName
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.data.commands.CommandStatus
import io.infinitic.workflowManager.common.data.workflows.WorkflowMethodOutput

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TaskDispatched::class, name = "TASK_DISPATCHED"),
    JsonSubTypes.Type(value = ChildWorkflowDispatched::class, name = "CHILD_WORKFLOW_DISPATCHED"),
    JsonSubTypes.Type(value = DelayWaited::class, name = "DELAY_WAITED"),
    JsonSubTypes.Type(value = EventWaited::class, name = "EVENT_WAITED"),
    JsonSubTypes.Type(value = InstantTaskRun::class, name = "INSTANT_TASK_RUN"),
    JsonSubTypes.Type(value = WorkflowPaused::class, name = "WORKFLOW_PAUSED"),
    JsonSubTypes.Type(value = WorkflowResumed::class, name = "WORKFLOW_RESUMED"),
    JsonSubTypes.Type(value = WorkflowCompleted::class, name = "WORKFLOW_COMPLETED"),
    JsonSubTypes.Type(value = WorkflowTerminated::class, name = "WORKFLOW_TERMINATED"),
    JsonSubTypes.Type(value = EventSent::class, name = "EVENT_SENT")
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class PastCommand(
    open val commandIndex: CommandIndex,
    open val commandHash: CommandHash,
    open val commandStatus: CommandStatus,
    open val decidedAt: DateTime
)

data class TaskDispatched(
    val taskId: TaskId,
    var taskOutput: TaskOutput?,
    override val commandIndex: CommandIndex,
    override val commandHash: CommandHash,
    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED,
    override val decidedAt: DateTime
) : PastCommand(commandIndex, commandHash, commandStatus, decidedAt)

data class ChildWorkflowDispatched(
    val childWorkflowId: WorkflowId,
    var childWorkflowOutput: WorkflowMethodOutput?,
    override val commandIndex: CommandIndex,
    override val commandHash: CommandHash,
    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED,
    override val decidedAt: DateTime
) : PastCommand(commandIndex, commandHash, commandStatus, decidedAt)

data class DelayWaited(
    val delayId: DelayId,
    override val commandIndex: CommandIndex,
    override val commandHash: CommandHash,
    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED,
    override val decidedAt: DateTime
) : PastCommand(commandIndex, commandHash, commandStatus, decidedAt)

data class EventWaited(
    val eventId: EventId,
    val eventName: EventName,
    var eventData: EventData?,
    override val commandIndex: CommandIndex,
    override val commandHash: CommandHash,
    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED,
    override val decidedAt: DateTime
) : PastCommand(commandIndex, commandHash, commandStatus, decidedAt)

/**
 * InstantTask have already been processed by the Decider
 */
data class InstantTaskRun(
    var taskOutput: TaskOutput,
    override val commandIndex: CommandIndex,
    override val commandHash: CommandHash,
    override val commandStatus: CommandStatus,
    override val decidedAt: DateTime
) : PastCommand(commandIndex, commandHash, commandStatus, decidedAt)

/**
 * EngineCommand are processed right away by the Engine
 */
sealed class PastEngineCommand(
    override val commandIndex: CommandIndex,
    override val commandHash: CommandHash,
    override val commandStatus: CommandStatus,
    override val decidedAt: DateTime
) : PastCommand(commandIndex, commandHash, commandStatus, decidedAt)

data class WorkflowPaused(
    override val commandIndex: CommandIndex,
    override val commandHash: CommandHash,
    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED,
    override val decidedAt: DateTime
) : PastEngineCommand(commandIndex, commandHash, commandStatus, decidedAt)

data class WorkflowResumed(
    override val commandIndex: CommandIndex,
    override val commandHash: CommandHash,
    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED,
    override val decidedAt: DateTime
) : PastEngineCommand(commandIndex, commandHash, commandStatus, decidedAt)

data class WorkflowCompleted(
    val workflowOutput: WorkflowMethodOutput?,
    override val commandIndex: CommandIndex,
    override val commandHash: CommandHash,
    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED,
    override val decidedAt: DateTime
) : PastEngineCommand(commandIndex, commandHash, commandStatus, decidedAt)

data class WorkflowTerminated(
    override val commandIndex: CommandIndex,
    override val commandHash: CommandHash,
    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED,
    override val decidedAt: DateTime
) : PastEngineCommand(commandIndex, commandHash, commandStatus, decidedAt)

data class EventSent(
    val eventName: EventName,
    var eventData: EventData?,
    override val commandIndex: CommandIndex,
    override val commandHash: CommandHash,
    override var commandStatus: CommandStatus = CommandStatus.DISPATCHED,
    override val decidedAt: DateTime
) : PastEngineCommand(commandIndex, commandHash, commandStatus, decidedAt)
