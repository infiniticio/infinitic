package com.zenaton.engine.workflows

import com.zenaton.engine.decisions.Message.DecisionDispatched
import com.zenaton.engine.tasks.Message.TaskDispatched

interface DispatcherInterface {
    fun getState(key: String): WorkflowState?
    fun updateState(state: WorkflowState)
    fun deleteState(state: WorkflowState)
    fun dispatchTask(msg: TaskDispatched)
    fun dispatchDecision(msg: DecisionDispatched)
    fun log()
}
