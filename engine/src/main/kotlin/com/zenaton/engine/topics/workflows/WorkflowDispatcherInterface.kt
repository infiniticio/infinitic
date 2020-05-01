package com.zenaton.engine.topics.workflows

import com.zenaton.engine.topics.decisions.messages.DecisionDispatched
import com.zenaton.engine.topics.delays.messages.DelayDispatched
import com.zenaton.engine.topics.tasks.messages.TaskDispatched
import com.zenaton.engine.topics.workflows.messages.WorkflowDispatched

interface WorkflowDispatcherInterface {
    fun dispatchTask(msg: TaskDispatched)
    fun dispatchChildWorkflow(msg: WorkflowDispatched)
    fun dispatchDelay(msg: DelayDispatched)
    fun dispatchDecision(msg: DecisionDispatched)
}
