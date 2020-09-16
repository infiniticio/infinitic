package io.infinitic.workflowManager.engine.engines.handlers

import io.infinitic.workflowManager.common.data.methodRuns.MethodRunId
import io.infinitic.workflowManager.common.messages.TaskCompleted
import io.infinitic.workflowManager.common.states.WorkflowState

abstract class MsgHandler {
    protected fun getMethodRun(state: WorkflowState, methodRunId: MethodRunId) =
        state.currentMethodRuns.first { it.methodRunId == methodRunId }
}
