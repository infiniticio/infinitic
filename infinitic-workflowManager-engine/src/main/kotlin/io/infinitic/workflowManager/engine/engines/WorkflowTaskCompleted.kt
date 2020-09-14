package io.infinitic.workflowManager.engine.engines

import io.infinitic.workflowManager.common.messages.WorkflowTaskCompleted
import io.infinitic.workflowManager.common.states.WorkflowState
import io.infinitic.workflowManager.engine.dispatcher.Dispatcher
import io.infinitic.workflowManager.engine.storages.WorkflowStateStorage
import org.slf4j.Logger

class WorkflowTaskCompleted(
    val logger: Logger,
    val storage: WorkflowStateStorage,
    val dispatcher: Dispatcher
) {
    fun handle(state: WorkflowState, msg: WorkflowTaskCompleted) {

    }
}
