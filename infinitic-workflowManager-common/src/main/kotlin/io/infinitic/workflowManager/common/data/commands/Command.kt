package io.infinitic.workflowManager.common.data.commands

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.common.data.SerializedData
import io.infinitic.common.json.Json
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.workflowManager.common.data.workflows.WorkflowMethod
import io.infinitic.workflowManager.common.data.workflows.WorkflowMethodInput
import io.infinitic.workflowManager.common.data.workflows.WorkflowName

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DispatchTask::class, name = "DISPATCH_TASK"),
    JsonSubTypes.Type(value = DispatchChildWorkflow::class, name = "DISPATCH_CHILD_WORKFLOW")
//    JsonSubTypes.Type(value = WaitDelay::class, name = "WAIT_DELAY"),
//    JsonSubTypes.Type(value = WaitEvent::class, name = "WAIT_EVENT")
//    JsonSubTypes.Type(value = RunInstantTask::class, name = "RUN_INSTANT_TASK"),
//    JsonSubTypes.Type(value = PauseWorkflow::class, name = "PAUSE_WORKFLOW"),
//    JsonSubTypes.Type(value = ResumeWorkflow::class, name = "RESUME_WORKFLOW"),
//    JsonSubTypes.Type(value = CompleteWorkflow::class, name = "COMPLETE_WORKFLOW"),
//    JsonSubTypes.Type(value = TerminateWorkflow::class, name = "TERMINATE_WORKFLOW"),
//    JsonSubTypes.Type(value = SendEvent::class, name = "SEND_EVENT")
)
@JsonIgnoreProperties(ignoreUnknown = true)

data class HashedCommand(
    val command: Command,
    val commandHash: CommandHash,
    val commandIndex: CommandIndex
)

sealed class Command(
    open val commandIndex: CommandIndex
) {
    fun hash(): CommandHash {
//        println(Json.stringify(this))
        val hash = CommandHash(SerializedData.from(this).hash())
//        println(hash)
        return hash
    }
}

data class DispatchTask(
    override val commandIndex: CommandIndex,
    val taskName: TaskName,
    val taskInput: TaskInput
) : Command(commandIndex)

data class DispatchChildWorkflow(
    override val commandIndex: CommandIndex,
    val childWorkflowName: WorkflowName,
    val childWorkflowMethod: WorkflowMethod,
    val childWorkflowMethodInput: WorkflowMethodInput
) : Command(commandIndex)

// data class WaitDelay(
//    override val commandIndex: CommandIndex
// ) : Command(commandIndex)
//
// data class WaitEvent(
//    override val commandIndex: CommandIndex
// ) : EngineCommand(commandIndex)

/**
 * InstantTask have already been processed by the Decider
 */
// data class RunInstantTask(
//    override val commandIndex: CommandIndex
// ) : Command(commandIndex)

/**
 * EngineCommand are processed right away by the Engine
 */
// sealed class EngineCommand(
//    override val commandIndex: CommandIndex
// ) : Command(commandIndex)
//
// data class PauseWorkflow(
//    override val commandIndex: CommandIndex
// ) : EngineCommand(commandIndex)
//
// data class ResumeWorkflow(
//    override val commandIndex: CommandIndex
// ) : EngineCommand(commandIndex)
//
// data class CompleteWorkflow(
//    override val commandIndex: CommandIndex
// ) : EngineCommand(commandIndex)
//
// data class TerminateWorkflow(
//    override val commandIndex: CommandIndex
// ) : EngineCommand(commandIndex)
//
// data class SendEvent(
//    override val commandIndex: CommandIndex
// ) : EngineCommand(commandIndex)
