package com.zenaton.workflowengine.topics.workflows.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobOutput
import com.zenaton.workflowengine.data.WorkflowId
import com.zenaton.workflowengine.topics.workflows.interfaces.WorkflowMessageInterface

data class TaskCompleted(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    val jobId: JobId,
    val jobOutput: JobOutput?
) : WorkflowMessageInterface
