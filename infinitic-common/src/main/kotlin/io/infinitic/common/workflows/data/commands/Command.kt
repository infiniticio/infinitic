/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.workflows.data.commands

import com.fasterxml.jackson.annotation.JsonProperty
import io.infinitic.common.data.methods.MethodInput
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.workflows.data.workflows.WorkflowName
import kotlinx.serialization.Serializable
import java.lang.reflect.Method

@Serializable
sealed class Command {
    fun hash() = CommandHash(SerializedData.from(this).hash())
}

/**
 * Commands are asynchronously processed
 */

@Serializable
data class DispatchTask(
    val taskName: TaskName,
    val methodName: MethodName,
    val methodParameterTypes: MethodParameterTypes,
    val methodInput: MethodInput,
    val taskMeta: TaskMeta
) : Command() {
    companion object {
        fun from(method: Method, args: Array<out Any>) = DispatchTask(
            taskName = TaskName.from(method),
            methodInput = MethodInput.from(method, args),
            methodParameterTypes = MethodParameterTypes.from(method),
            methodName = MethodName.from(method),
            taskMeta = TaskMeta()
        )
    }
}

@Serializable
data class DispatchChildWorkflow(
    val childWorkflowName: WorkflowName,
    val childMethodName: MethodName,
    val childMethodParameterTypes: MethodParameterTypes,
    val childMethodInput: MethodInput
) : Command() {
    companion object {
        fun from(method: Method, args: Array<out Any>) = DispatchChildWorkflow(
            childWorkflowName = WorkflowName.from(method),
            childMethodName = MethodName.from(method),
            childMethodParameterTypes = MethodParameterTypes.from(method),
            childMethodInput = MethodInput.from(method, args)
        )
    }
}

@Serializable
object StartAsync : Command() {
    // as we can not define a data class without parameter, we add manually the equals func
    override fun equals(other: Any?) = javaClass == other?.javaClass
}

@Serializable
data class EndAsync(
    @JsonProperty("output")
    val asyncOutput: CommandOutput
) : Command()

@Serializable
object StartInlineTask : Command() {
    // as we can not define a data class without parameter, we add manually the equals func
    override fun equals(other: Any?) = javaClass == other?.javaClass
}

@Serializable
data class EndInlineTask(
    @JsonProperty("output")
    val inlineTaskOutput: CommandOutput
) : Command()

@Serializable
data class DispatchTimer(
    val duration: Int
) : Command()

@Serializable
data class DispatchReceiver(
    val klass: String
) : Command()
