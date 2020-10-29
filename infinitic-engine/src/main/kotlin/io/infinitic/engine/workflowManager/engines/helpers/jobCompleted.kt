// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.engine.workflowManager.engines.helpers

import io.infinitic.common.data.interfaces.plus
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandOutput
import io.infinitic.common.workflows.data.commands.CommandStatusCompleted
import io.infinitic.common.workflows.data.commands.CommandStatusOngoing
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.states.WorkflowState
import io.infinitic.messaging.api.dispatcher.Dispatcher

suspend fun jobCompleted(
    dispatcher: Dispatcher,
    state: WorkflowState,
    methodRunId: MethodRunId,
    commandId: CommandId,
    commandOutput: CommandOutput
) {
    val methodRun = getMethodRun(state, methodRunId)
    val pastCommand = getPastCommand(methodRun, commandId)

    // do nothing if this command is not ongoing (could have been canceled)
    if (pastCommand.commandStatus !is CommandStatusOngoing) return

    // update command status
    pastCommand.commandStatus = CommandStatusCompleted(commandOutput, state.workflowTaskIndex)

    // trigger a new workflow task for the first step solved by this command
    // note: pastSteps is naturally ordered by time => the first branch completed is the earliest step
    methodRun.pastSteps
        .find { it.isTerminatedBy(pastCommand) }
        ?.also {
            // update pastStep with a copy (!) of current properties and anticipated workflowTaskIndex
            it.propertiesNameHashAtTermination = state.currentPropertiesNameHash.copy()
            it.workflowTaskIndexAtTermination = state.workflowTaskIndex + 1
            // dispatch a new workflowTask
            dispatchWorkflowTask(dispatcher, state, methodRun, it.stepPosition)
            // keep this command as we could have another pastStep solved by it
            state.bufferedCommands.add(pastCommand.commandId)
        }
        ?: cleanMethodRunIfNeeded(methodRun, state) // if everything is completed in methodRun then filter state
}
