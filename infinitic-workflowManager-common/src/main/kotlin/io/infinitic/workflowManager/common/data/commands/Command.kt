package io.infinitic.workflowManager.common.data.commands

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.workflowManager.common.data.methodRuns.MethodName
import io.infinitic.workflowManager.common.data.methodRuns.MethodInput
import io.infinitic.workflowManager.common.data.workflows.WorkflowName

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DispatchTask::class, name = "DISPATCH_TASK"),
    JsonSubTypes.Type(value = DispatchChildWorkflow::class, name = "DISPATCH_CHILD_WORKFLOW"),
    JsonSubTypes.Type(value = StartAsync::class, name = "START_ASYNC"),
    JsonSubTypes.Type(value = EndAsync::class, name = "END_ASYNC"),
    JsonSubTypes.Type(value = StartInlineTask::class, name = "START_INLINE_TASK"),
    JsonSubTypes.Type(value = EndInlineTask::class, name = "END_INLINE_TASK"),
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
    @JsonProperty("name")
    val taskName: TaskName,
    @JsonProperty("input")
    val taskInput: TaskInput,
    @JsonProperty("meta")
    val taskMeta: TaskMeta
) : Command()

data class DispatchChildWorkflow(
    @JsonProperty("name")
    val childWorkflowName: WorkflowName,
    @JsonProperty("method")
    val childMethodName: MethodName,
    @JsonProperty("input")
    val childMethodInput: MethodInput
) : Command()

object StartAsync : Command() {
    // as we can not define a data class without parameter, we add manually the equals func
    override fun equals(other: Any?) = javaClass == other?.javaClass
}

data class EndAsync(
    @JsonProperty("output")
    val asyncOutput: CommandOutput
) : Command()

object StartInlineTask : Command() {
    // as we can not define a data class without parameter, we add manually the equals func
    override fun equals(other: Any?) = javaClass == other?.javaClass
}
data class EndInlineTask(
    @JsonProperty("output")
    val inlineTaskOutput: CommandOutput
) : Command()

data class DispatchTimer(
    val duration: Int
) : Command()

data class DispatchReceiver(
    val klass: String
) : Command()
