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
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandType
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
    var methodReturnValue: MethodReturnValue? = null,
    val workflowTaskIndexAtStart: WorkflowTaskIndex = WorkflowTaskIndex(0),
    val propertiesNameHashAtStart: Map<PropertyName, PropertyHash>,
    val pastCommands: MutableList<PastCommand> = mutableListOf(),
    val pastSteps: MutableList<PastStep> = mutableListOf()
) {
    /**
     * Retrieve step by position
     * This value could be null (eg. for starting position of async command)
     */
    fun getStepByPosition(position: MethodRunPosition): PastStep? =
        pastSteps.firstOrNull { it.stepPosition == position }

    /**
     * Retrieve step by position
     * This value could be null (eg. for starting position of async command)
     */
    fun getCommandByPosition(position: MethodRunPosition): PastCommand? =
        pastCommands.firstOrNull { it.commandPosition == position }

    /**
     * Retrieve pastCommand per commandId.
     * This value should always be present.
     */
    fun getPastCommand(commandId: CommandId): PastCommand =
        pastCommands.first { it.commandId == commandId }

    /**
     * To be terminated, a method should provide a return value and have all steps and branches terminated.
     * We add a constraint on child-workflows as well to ensure that this methodRun is not deleted
     * before child workflows are completed, so that child workflows can be canceled
     * if the workflow itself is canceled
     */
    fun isTerminated() = methodReturnValue != null &&
        pastSteps.all { it.isTerminated() } &&
        pastCommands
            .filter { it.commandType == CommandType.DISPATCH_CHILD_WORKFLOW || it.commandType == CommandType.START_ASYNC }
            .all { it.isTerminated() }
}
