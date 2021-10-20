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

package io.infinitic.common.workflows.data.methodRuns

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.DispatchMethodCommand
import io.infinitic.common.workflows.data.commands.DispatchTaskCommand
import io.infinitic.common.workflows.data.commands.DispatchWorkflowCommand
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.steps.PastStep
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import kotlinx.serialization.Serializable

@Serializable
data class MethodRun(
    /**
     * clients synchronously waiting for the returned value
     */
    val waitingClients: MutableSet<ClientName>,

    val methodRunId: MethodRunId,
    val parentWorkflowId: WorkflowId?,
    val parentWorkflowName: WorkflowName?,
    val parentMethodRunId: MethodRunId?,
    val methodName: MethodName,
    val methodParameterTypes: MethodParameterTypes?,
    val methodParameters: MethodParameters,
    var methodReturnValue: ReturnValue? = null,
    val workflowTaskIndexAtStart: WorkflowTaskIndex,
    val propertiesNameHashAtStart: Map<PropertyName, PropertyHash>,
    val pastCommands: MutableList<PastCommand> = mutableListOf(),
    val pastSteps: MutableList<PastStep> = mutableListOf(),
    var currentStep: PastStep? = null
) {
    /**
     * Retrieve step by position
     */
    fun getStepByPosition(position: MethodRunPosition): PastStep? =
        pastSteps.firstOrNull { it.stepPosition == position }

    /**
     * Retrieve pastCommand per commandId.
     */
    fun getPastCommand(commandId: CommandId): PastCommand? =
        pastCommands.firstOrNull { it.commandId == commandId }

    /**
     * To be terminated, a method should provide a return value and have all steps and branches terminated.
     * We ensure that this methodRun is not deleted before children are terminated
     * - child workflows should be canceled if the workflow itself is canceled
     * - other methods could use reference to deferred owned by current method
     */
    fun isTerminated() = methodReturnValue != null &&
        pastSteps.all { it.isTerminated() } &&
        pastCommands.filter {
            when (it.command) {
                is DispatchWorkflowCommand,
                is DispatchMethodCommand,
                is DispatchTaskCommand -> true
                else -> false
            }
        }.all { it.isTerminated() }
}
