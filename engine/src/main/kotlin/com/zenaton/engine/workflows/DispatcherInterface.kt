package com.zenaton.engine.workflows

import com.zenaton.engine.decisions.DecisionDispatched
import com.zenaton.engine.delays.DelayDispatched
import com.zenaton.engine.tasks.TaskDispatched

interface DispatcherInterface {
    fun dispatchTask(msg: TaskDispatched)
    fun dispatchWorkflow(msg: WorkflowDispatched)
    fun dispatchDelay(msg: DelayDispatched)
    fun dispatchDecision(msg: DecisionDispatched)
}
