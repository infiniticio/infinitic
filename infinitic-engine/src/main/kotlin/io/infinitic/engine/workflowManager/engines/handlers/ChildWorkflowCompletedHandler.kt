package io.infinitic.engine.workflowManager.engines.handlers

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.workflowManager.data.commands.CommandId
import io.infinitic.common.workflowManager.data.commands.CommandOutput
import io.infinitic.common.workflowManager.data.commands.CommandStatusCompleted
import io.infinitic.common.workflowManager.data.commands.CommandStatusOngoing
import io.infinitic.common.workflowManager.data.states.WorkflowState
import io.infinitic.common.workflowManager.messages.ChildWorkflowCompleted

class ChildWorkflowCompletedHandler(
    override val dispatcher: Dispatcher
) : MsgHandler(dispatcher) {
    suspend fun handle(state: WorkflowState, msg: ChildWorkflowCompleted) {
        val methodRun = getMethodRun(state, msg.methodRunId)

        // update command status
        val commandId = CommandId(msg.childWorkflowId)
        val pastCommand = methodRun.pastCommands.first { it.commandId == commandId }

        // do nothing if this command is not ongoing (could have been canceled)
        if (pastCommand.commandStatus !is CommandStatusOngoing) return

        // update command status
        pastCommand.commandStatus = CommandStatusCompleted(
            CommandOutput(msg.childWorkflowOutput.data),
            state.currentMessageIndex
        )

        // update steps
        val justCompleted = methodRun.pastSteps
            .map { it.terminateBy(pastCommand, state.currentProperties) }
            .any { it } // any must be applied only after having applied terminateBy to all elements

        if (justCompleted) {
            dispatchWorkflowTask(state, methodRun)
        }

        // if everything is completed in methodRun then filter state
        cleanMethodRun(methodRun, state)
    }
}
