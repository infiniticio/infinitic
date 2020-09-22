package io.infinitic.workflowManager.engine.engines.handlers

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.workflowManager.common.data.commands.CommandId
import io.infinitic.workflowManager.common.data.commands.CommandOutput
import io.infinitic.workflowManager.common.data.commands.CommandStatusCompleted
import io.infinitic.workflowManager.common.data.commands.CommandStatusOngoing
import io.infinitic.workflowManager.common.messages.TaskCompleted
import io.infinitic.workflowManager.common.data.states.WorkflowState

class TaskCompletedHandler(
    override val dispatcher: Dispatcher
) : MsgHandler(dispatcher) {
    suspend fun handle(state: WorkflowState, msg: TaskCompleted) {
        val methodRun = getMethodRun(state, msg.methodRunId)

        // update command status
        val commandId = CommandId(msg.taskId)
        val pastCommand = methodRun.pastCommands.first { it.commandId == commandId }

        // do nothing if this command is not ongoing (could have been canceled)
        if (pastCommand.commandStatus !is CommandStatusOngoing) return

        // update command status
        pastCommand.commandStatus = CommandStatusCompleted(
            CommandOutput(msg.taskOutput.data),
            state.currentMessageIndex
        )

        // update steps
        val justCompleted = methodRun.pastSteps
            .map { it.terminateBy(pastCommand, state.currentProperties) }
            .any() // any[} must be applied only after having applied terminateBy to all elements

        if (justCompleted) {
            dispatchWorkflowTask(state, methodRun)
        }

        // if everything is completed in methodRun then filter state
        if (methodRun.methodOutput != null &&
            methodRun.pastCommands.all { it.isTerminated() } &&
            methodRun.pastSteps.all { it.isTerminated() }
        ) {
            // TODO("filter workflow if unused properties")
            state.currentMethodRuns.remove(methodRun)
        }
    }
}
