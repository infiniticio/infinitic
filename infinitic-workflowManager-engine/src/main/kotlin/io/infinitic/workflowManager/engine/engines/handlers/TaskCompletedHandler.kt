package io.infinitic.workflowManager.engine.engines.handlers

import io.infinitic.workflowManager.common.data.commands.CommandId
import io.infinitic.workflowManager.common.data.commands.CommandStatusCompleted
import io.infinitic.workflowManager.common.data.instructions.PastCommand
import io.infinitic.workflowManager.common.data.instructions.PastStep
import io.infinitic.workflowManager.common.messages.TaskCompleted
import io.infinitic.workflowManager.common.states.WorkflowState
import io.infinitic.workflowManager.engine.dispatcher.Dispatcher
import io.infinitic.workflowManager.engine.storages.WorkflowStateStorage

class TaskCompletedHandler(
    val storage: WorkflowStateStorage,
    val dispatcher: Dispatcher
) : MsgHandler() {
    suspend fun handle(state: WorkflowState, msg: TaskCompleted) {
        val methodRun = getMethodRun(state, msg.methodRunId)

        // update command status
        val commandId = CommandId(msg.taskId)
        val pastCommand = methodRun.pastInstructionsInMethod
            .filterIsInstance<PastCommand>()
            .first { it.commandId == commandId }
        pastCommand.commandStatus = CommandStatusCompleted(msg.taskOutput.data, state.currentEventIndex)

        // update step (if any)
        val stepCompleted = methodRun.pastInstructionsInMethod
            .filterIsInstance<PastStep>()
            .any {
                it.completeCommand(msg.taskId, state.currentProperties)
            }
    }
}
