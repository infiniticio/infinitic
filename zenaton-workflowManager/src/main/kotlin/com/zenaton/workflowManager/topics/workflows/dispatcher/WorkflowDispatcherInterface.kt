package com.zenaton.workflowManager.topics.workflows.dispatcher

import com.zenaton.workflowManager.topics.workflows.interfaces.WorkflowMessageInterface

interface WorkflowDispatcherInterface {
    fun dispatch(msg: WorkflowMessageInterface, after: Float = 0F)
}
