package io.infinitic.workflowManager.engine.engines.handlers

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.workflowManager.common.data.commands.CommandId
import io.infinitic.workflowManager.common.data.commands.CommandStatusCompleted
import io.infinitic.workflowManager.common.data.commands.CommandStatusOngoing
import io.infinitic.workflowManager.common.data.instructions.PastCommand
import io.infinitic.workflowManager.common.data.instructions.PastStep
import io.infinitic.workflowManager.common.messages.TaskCompleted
import io.infinitic.workflowManager.common.states.WorkflowState
import io.infinitic.workflowManager.engine.storages.WorkflowStateStorage

class TaskCompletedHandler(
    override val dispatcher: Dispatcher
) : MsgHandler(dispatcher) {
    suspend fun handle(state: WorkflowState, msg: TaskCompleted) {
        val methodRun = getMethodRun(state, msg.methodRunId)

        // update command status
        val commandId = CommandId(msg.taskId)
        val pastCommand = methodRun.pastInstructions
            .filterIsInstance<PastCommand>()
            .first { it.commandId == commandId }

        // do nothing if this command is not ongoing (could have been canceled)
        if (pastCommand.commandStatus !is CommandStatusOngoing) return

        // update command status
        pastCommand.commandStatus = CommandStatusCompleted(msg.taskOutput.data, state.currentMessageIndex)

        // update steps
        val justCompleted = methodRun.pastInstructions
            .filterIsInstance<PastStep>()
            .map { it.terminateBy(pastCommand, state.currentProperties) }
            .any { it }

        if (justCompleted) {
            dispatchWorkflowTask(state, methodRun)
        }

        // if everything is completed in methodRun then filter state
        if (methodRun.methodOutput != null && methodRun.pastInstructions.all { it.isTerminated() }) {
            // TODO("filter workflow if unused properties")
            state.currentMethodRuns.remove(methodRun)
        }
    }
}
