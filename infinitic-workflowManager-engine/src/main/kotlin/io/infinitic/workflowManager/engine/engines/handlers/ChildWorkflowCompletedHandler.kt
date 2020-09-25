package io.infinitic.workflowManager.engine.engines.handlers

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.workflowManager.common.data.commands.CommandId
import io.infinitic.workflowManager.common.data.commands.CommandOutput
import io.infinitic.workflowManager.common.data.commands.CommandStatusCompleted
import io.infinitic.workflowManager.common.data.commands.CommandStatusOngoing
import io.infinitic.workflowManager.common.data.states.WorkflowState
import io.infinitic.workflowManager.common.messages.ChildWorkflowCompleted

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
