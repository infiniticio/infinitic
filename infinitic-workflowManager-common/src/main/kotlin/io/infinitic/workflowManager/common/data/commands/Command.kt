package io.infinitic.workflowManager.common.data.commands

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.workflowManager.common.data.methods.Method
import io.infinitic.workflowManager.common.data.methods.MethodInput
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
sealed class Command {
    fun hash() = CommandHash(SerializedData.from(this).hash())
}

/**
 * DeferCommands are asynchronously processed by Workers
 */

sealed class DeferCommand : Command()

data class DispatchTask(
    val taskName: TaskName,
    val taskInput: TaskInput
) : DeferCommand()

data class DispatchChildWorkflow(
    val childWorkflowName: WorkflowName,
    val childMethod: Method,
    val childMethodInput: MethodInput
) : DeferCommand()

// data class DispatchTimer(
// ) : DeferCommand()
//
// data class DispatchReceiver(
// ) : DeferCommand()

/**
 * InlineTask are processed within WorkflowTasks
 */
// data class RunInlineTask(
// ) : Command()

/**
 * EngineCommand are processed by the Engine
 */
// sealed class EngineCommand(
// ) : Command()
//
// data class PauseWorkflow(
// ) : EngineCommand()
//
// data class ResumeWorkflow(
// ) : EngineCommand()
//
// data class CompleteWorkflow(
// ) : EngineCommand()
//
// data class TerminateWorkflow(
// ) : EngineCommand()
//
// data class SendEvent(
// ) : EngineCommand()
