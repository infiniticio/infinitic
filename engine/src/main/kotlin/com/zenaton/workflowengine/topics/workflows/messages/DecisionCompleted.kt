package com.zenaton.workflowengine.topics.workflows.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.decisionmanager.data.DecisionId
import com.zenaton.decisionmanager.data.DecisionOutput
import com.zenaton.workflowengine.data.WorkflowId
import com.zenaton.workflowengine.topics.workflows.interfaces.WorkflowMessageInterface

data class DecisionCompleted(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    val decisionId: DecisionId,
    val decisionOutput: DecisionOutput
) : WorkflowMessageInterface
