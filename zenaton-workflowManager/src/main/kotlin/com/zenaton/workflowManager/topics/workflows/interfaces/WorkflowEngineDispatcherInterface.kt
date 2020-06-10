package com.zenaton.workflowManager.topics.workflows.interfaces

import com.zenaton.decisionmanager.messages.DecisionDispatched
import com.zenaton.jobManager.messages.DispatchJob
import com.zenaton.workflowManager.topics.delays.messages.DelayDispatched
import com.zenaton.workflowManager.messages.WorkflowDispatched

interface WorkflowEngineDispatcherInterface {
    fun dispatch(msg: DispatchJob, after: Float = 0f)
    fun dispatch(msg: WorkflowDispatched, after: Float = 0f)
    fun dispatch(msg: DelayDispatched, after: Float = 0f)
    fun dispatch(msg: DecisionDispatched, after: Float = 0f)
}
