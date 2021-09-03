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

package io.infinitic.workflows.engine.helpers

import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflowTasks.plus
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.workflows.engine.output.WorkflowEngineOutput
import kotlinx.coroutines.CoroutineScope

internal fun CoroutineScope.commandTerminated(
    workflowEngineOutput: WorkflowEngineOutput,
    state: WorkflowState,
    methodRunId: MethodRunId,
    commandId: CommandId,
    commandStatus: CommandStatus
) {
    val methodRun = state.getMethodRun(methodRunId)!!
    val pastCommand = methodRun.getPastCommand(commandId)

    // do nothing if this command is already terminated (canceled or completed, failed is considered transient)
    if (pastCommand.isTerminated()) return

    // update command status
    pastCommand.commandStatus = commandStatus

    // trigger a new workflow task for the *first* step solved by this command
    // note: pastSteps is ordered per workflowTaskIndex (time) => the first completed step is the earliest
    val pastStep = methodRun.pastSteps.find { it.isTerminatedBy(pastCommand) }

    if (pastStep != null) {
        // update pastStep with a copy (!) of current properties and anticipated workflowTaskIndex
        pastStep.propertiesNameHashAtTermination = state.currentPropertiesNameHash.toMap()
        pastStep.workflowTaskIndexAtTermination = state.workflowTaskIndex + 1

        // dispatch a new workflowTask
        dispatchWorkflowTask(
            workflowEngineOutput,
            state,
            methodRun,
            pastStep.stepPosition
        )

        // keep this command as we could have another pastStep solved by it
        state.bufferedCommands.add(pastCommand.commandId)
    }
}
