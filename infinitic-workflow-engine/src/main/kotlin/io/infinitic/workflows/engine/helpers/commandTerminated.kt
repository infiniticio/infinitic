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

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.workflows.engine.output.WorkflowEngineOutput
import kotlinx.coroutines.CoroutineScope

/**
 * Return true if this command terminates a step
 */

// trigger a new workflow task for the *first* step solved by this command
// note: pastSteps is ordered per workflowTaskIndex (time) => the first completed step is the earliest
internal fun CoroutineScope.commandTerminated(
    output: WorkflowEngineOutput,
    state: WorkflowState,
    methodRunId: MethodRunId,
    commandId: CommandId,
    commandStatus: CommandStatus
) {
    val methodRun = state.getMethodRun(methodRunId) ?: thisShouldNotHappen()
    val pastCommand = state.getPastCommand(commandId, methodRun)

    // do nothing if this command is already terminated
    // (i.e. canceled or completed, not failed as it's a transient status)
    if (pastCommand.isTerminated()) return

    // update command status
    pastCommand.commandStatus = commandStatus

    if (stepTerminated(output, state, pastCommand)) {
        // keep this command as we could have another pastStep solved by it
        state.runningTerminatedCommands.add(0, commandId)
    }
}

// search the first step completed by this command
internal fun CoroutineScope.stepTerminated(
    output: WorkflowEngineOutput,
    state: WorkflowState,
    pastCommand: PastCommand
): Boolean {
    // get all methodRuns terminated by this command
    val methodRuns = state.methodRuns.filter { it.currentStep?.isTerminatedBy(pastCommand) == true }

    // get step with lowest workflowTaskIndexAtStart
    methodRuns.minByOrNull { it.currentStep!!.workflowTaskIndexAtStart }?.let {
        val pastStep = it.currentStep!!
        // terminate step
        pastStep.updateWith(pastCommand)
        // update pastStep with a copy (!) of current properties and anticipated workflowTaskIndex
        pastStep.propertiesNameHashAtTermination = state.currentPropertiesNameHash.toMap()
        pastStep.workflowTaskIndexAtTermination = state.workflowTaskIndex + 1

        // we need to add this check to handle the case of ongoing task failure
        if (pastStep.isTerminated()) {
            it.pastSteps.add(pastStep)
            it.currentStep = null
        }

        // dispatch a new workflowTask
        dispatchWorkflowTask(
            output,
            state,
            it,
            pastStep.stepPosition
        )

        // if more than 1 methodRun is completed by this command, we need to keep it
        return methodRuns.size > 1
    }

    return false
}
