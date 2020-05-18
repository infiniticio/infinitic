package com.zenaton.workflowengine.topics.workflows.dispatcher

import com.zenaton.workflowengine.topics.workflows.interfaces.WorkflowMessageInterface

interface WorkflowDispatcherInterface {
    fun dispatch(msg: WorkflowMessageInterface, after: Float = 0F)
}
