package com.zenaton.engine.topics.workflows.interfaces

import com.zenaton.engine.topics.decisions.messages.DecisionDispatched
import com.zenaton.engine.topics.delays.messages.DelayDispatched
import com.zenaton.engine.topics.workflows.messages.WorkflowDispatched
import com.zenaton.taskmanager.messages.TaskDispatched

interface WorkflowEngineDispatcherInterface {
    fun dispatch(msg: TaskDispatched, after: Float = 0f)
    fun dispatch(msg: WorkflowDispatched, after: Float = 0f)
    fun dispatch(msg: DelayDispatched, after: Float = 0f)
    fun dispatch(msg: DecisionDispatched, after: Float = 0f)
}
