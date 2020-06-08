package com.zenaton.workflowengine.topics.workflows.interfaces

import com.zenaton.decisionmanager.messages.DecisionDispatched
import com.zenaton.jobManager.messages.DispatchJob
import com.zenaton.workflowengine.topics.delays.messages.DelayDispatched
import com.zenaton.workflowengine.topics.workflows.messages.WorkflowDispatched

interface WorkflowEngineDispatcherInterface {
    fun dispatch(msg: DispatchJob, after: Float = 0f)
    fun dispatch(msg: WorkflowDispatched, after: Float = 0f)
    fun dispatch(msg: DelayDispatched, after: Float = 0f)
    fun dispatch(msg: DecisionDispatched, after: Float = 0f)
}
