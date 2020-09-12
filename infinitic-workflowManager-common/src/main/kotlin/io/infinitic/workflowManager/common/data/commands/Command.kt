package io.infinitic.workflowManager.common.data.commands

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.workflowManager.common.data.methods.MethodName
import io.infinitic.workflowManager.common.data.methods.MethodInput
import io.infinitic.workflowManager.common.data.workflows.WorkflowName

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DispatchTask::class, name = "DISPATCH_TASK"),
    JsonSubTypes.Type(value = DispatchChildWorkflow::class, name = "DISPATCH_CHILD_WORKFLOW"),
    JsonSubTypes.Type(value = DispatchTimer::class, name = "DISPATCH_TIMER"),
    JsonSubTypes.Type(value = DispatchReceiver::class, name = "DISPATCH_RECEIVER")
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class Command {
    fun hash() = CommandHash(SerializedData.from(this).hash())
}

/**
 * Commands are asynchronously processed
 */

data class DispatchTask(
    val taskName: TaskName,
    val taskInput: TaskInput
) : Command()

data class DispatchChildWorkflow(
    val childWorkflowName: WorkflowName,
    val childMethodName: MethodName,
    val childMethodInput: MethodInput
) : Command()

data class DispatchTimer(
    val duration: Int
) : Command()

data class DispatchReceiver(
    val klass: String
) : Command()

