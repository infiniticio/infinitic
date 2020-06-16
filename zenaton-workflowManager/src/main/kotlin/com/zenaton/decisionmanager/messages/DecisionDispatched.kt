package com.zenaton.decisionmanager.messages

import com.zenaton.decisionmanager.data.DecisionData
import com.zenaton.decisionmanager.data.DecisionId
import com.zenaton.decisionmanager.data.DecisionName
import com.zenaton.decisionmanager.messages.interfaces.DecisionMessageInterface
import com.zenaton.workflowengine.data.WorkflowId

data class DecisionDispatched(
    override var decisionId: DecisionId,
    val decisionName: DecisionName,
    val decisionData: DecisionData?,
    val workflowId: WorkflowId? = null
) : DecisionMessageInterface
