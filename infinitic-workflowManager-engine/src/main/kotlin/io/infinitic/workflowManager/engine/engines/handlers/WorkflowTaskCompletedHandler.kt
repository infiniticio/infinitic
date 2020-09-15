package io.infinitic.workflowManager.engine.engines.handlers

import io.infinitic.workflowManager.common.messages.WorkflowTaskCompleted
import io.infinitic.workflowManager.common.states.WorkflowState
import io.infinitic.workflowManager.engine.dispatcher.Dispatcher
import io.infinitic.workflowManager.engine.storages.WorkflowStateStorage

class WorkflowTaskCompletedHandler(
    val storage: WorkflowStateStorage,
    val dispatcher: Dispatcher
) {
    fun handle(state: WorkflowState, msg: WorkflowTaskCompleted) {
        val workflowTaskOutput = msg.workflowTaskOutput
        // add new commands to MethodRun

        // add new steps to MethodRun

        // update current workflow properties

        // is methodRun is completed, filter state

        // apply buffered messages

        // update state
    }
}
