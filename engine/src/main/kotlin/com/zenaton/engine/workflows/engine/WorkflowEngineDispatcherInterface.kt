package com.zenaton.engine.workflows.engine

import com.zenaton.engine.decisions.messages.DecisionDispatched
import com.zenaton.engine.delays.messages.DelayDispatched
import com.zenaton.engine.tasks.messages.TaskDispatched
import com.zenaton.engine.workflows.messages.WorkflowDispatched

interface WorkflowEngineDispatcherInterface {
    fun dispatch(msg: TaskDispatched, after: Float = 0f)
    fun dispatch(msg: WorkflowDispatched, after: Float = 0f)
    fun dispatch(msg: DelayDispatched, after: Float = 0f)
    fun dispatch(msg: DecisionDispatched, after: Float = 0f)
}
