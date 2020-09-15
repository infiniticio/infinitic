package io.infinitic.workflowManager.engine.engines.handlers

import io.infinitic.workflowManager.common.messages.TaskCompleted
import io.infinitic.workflowManager.common.states.WorkflowState
import io.infinitic.workflowManager.engine.dispatcher.Dispatcher
import io.infinitic.workflowManager.engine.storages.WorkflowStateStorage

class TaskCompletedHandler(
    val storage: WorkflowStateStorage,
    val dispatcher: Dispatcher
) {
    fun handle(state: WorkflowState, msg: TaskCompleted) {
        //
    }
}
