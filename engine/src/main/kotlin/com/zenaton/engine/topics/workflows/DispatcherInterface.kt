package com.zenaton.engine.topics.workflows

import com.zenaton.engine.topics.decisions.DecisionDispatched
import com.zenaton.engine.topics.delays.DelayDispatched
import com.zenaton.engine.topics.tasks.TaskDispatched

interface DispatcherInterface {
    fun dispatchTask(msg: TaskDispatched)
    fun dispatchWorkflow(msg: WorkflowDispatched)
    fun dispatchDelay(msg: DelayDispatched)
    fun dispatchDecision(msg: DecisionDispatched)
}
