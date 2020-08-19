package io.infinitic.workflowManager.common.data.commands

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.common.data.DateTime
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskOutput
import io.infinitic.workflowManager.common.data.DelayId
import io.infinitic.workflowManager.common.data.EventData
import io.infinitic.workflowManager.common.data.EventId
import io.infinitic.workflowManager.common.data.commands.CommandHash
import io.infinitic.workflowManager.common.data.EventName
import io.infinitic.workflowManager.common.data.WorkflowId
import io.infinitic.workflowManager.data.commands.CommandStatus
import io.infinitic.workflowManager.common.data.branches.BranchOutput

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DispatchTask::class, name = "DISPATCH_TASK"),
    JsonSubTypes.Type(value = DispatchChildWorkflow::class, name = "DISPATCH_CHILD_WORKFLOW"),
    JsonSubTypes.Type(value = WaitDelay::class, name = "WAIT_DELAY"),
    JsonSubTypes.Type(value = WaitEvent::class, name = "WAIT_EVENT"),
    JsonSubTypes.Type(value = InstantTask::class, name = "RUN_INSTANT_TASK"),
    JsonSubTypes.Type(value = PauseWorkflow::class, name = "PAUSE_WORKFLOW"),
    JsonSubTypes.Type(value = ResumeWorkflow::class, name = "RESUME_WORKFLOW"),
    JsonSubTypes.Type(value = CompleteWorkflow::class, name = "COMPLETE_WORKFLOW"),
    JsonSubTypes.Type(value = TerminateWorkflow::class, name = "TERMINATE_WORKFLOW"),
    JsonSubTypes.Type(value = SendEvent::class, name = "SEND_EVENT")
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class Command(
    open val decidedAt: DateTime,
    open val commandHash: CommandHash,
    open val actionStatus: CommandStatus
)

data class DispatchTask(
    val taskId: TaskId,
    var taskOutput: TaskOutput?,
    override val decidedAt: DateTime,
    override val commandHash: CommandHash,
    override var actionStatus: CommandStatus = CommandStatus.DISPATCHED
) : Command(decidedAt, commandHash, actionStatus)

data class DispatchChildWorkflow(
    val childWorkflowId: WorkflowId,
    var childWorkflowOutput: BranchOutput?,
    override val decidedAt: DateTime,
    override val commandHash: CommandHash,
    override var actionStatus: CommandStatus = CommandStatus.DISPATCHED
) : Command(decidedAt, commandHash, actionStatus)

data class WaitDelay(
    val delayId: DelayId,
    override val decidedAt: DateTime,
    override val commandHash: CommandHash,
    override var actionStatus: CommandStatus = CommandStatus.DISPATCHED
) : Command(decidedAt, commandHash, actionStatus)

data class WaitEvent(
    val eventId: EventId,
    val eventName: EventName,
    var eventData: EventData?,
    override val decidedAt: DateTime,
    override val commandHash: CommandHash,
    override var actionStatus: CommandStatus = CommandStatus.DISPATCHED
) : Command(decidedAt, commandHash, actionStatus)

/**
 * InstantTask have already been processed by the Decider
 */
data class InstantTask(
    override val decidedAt: DateTime,
    override val commandHash: CommandHash,
    override val actionStatus: CommandStatus,
    var taskOutput: TaskOutput
) : Command(decidedAt, commandHash, actionStatus)

/**
 * EngineCommand are processed right away by the Engine
 */
sealed class EngineCommand(
    override val decidedAt: DateTime,
    override val commandHash: CommandHash,
    override val actionStatus: CommandStatus
) : Command(decidedAt, commandHash, actionStatus)

data class PauseWorkflow(
    override val decidedAt: DateTime,
    override val commandHash: CommandHash,
    override var actionStatus: CommandStatus = CommandStatus.DISPATCHED
) : EngineCommand(decidedAt, commandHash, actionStatus)

data class ResumeWorkflow(
    override val decidedAt: DateTime,
    override val commandHash: CommandHash,
    override var actionStatus: CommandStatus = CommandStatus.DISPATCHED
) : EngineCommand(decidedAt, commandHash, actionStatus)

data class CompleteWorkflow(
    override val decidedAt: DateTime,
    override val commandHash: CommandHash,
    override var actionStatus: CommandStatus = CommandStatus.DISPATCHED,
    val workflowOutput: BranchOutput?
) : EngineCommand(decidedAt, commandHash, actionStatus)

data class TerminateWorkflow(
    override val decidedAt: DateTime,
    override val commandHash: CommandHash,
    override var actionStatus: CommandStatus = CommandStatus.DISPATCHED
) : EngineCommand(decidedAt, commandHash, actionStatus)

data class SendEvent(
    override val decidedAt: DateTime,
    override val commandHash: CommandHash,
    override var actionStatus: CommandStatus = CommandStatus.DISPATCHED,
    val eventName: EventName,
    var eventData: EventData?
) : EngineCommand(decidedAt, commandHash, actionStatus)
