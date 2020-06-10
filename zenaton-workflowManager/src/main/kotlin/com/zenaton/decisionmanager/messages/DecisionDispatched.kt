package com.zenaton.decisionmanager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.decisionmanager.data.DecisionData
import com.zenaton.decisionmanager.data.DecisionId
import com.zenaton.decisionmanager.data.DecisionName
import com.zenaton.decisionmanager.messages.interfaces.DecisionMessageInterface
import com.zenaton.workflowManager.data.WorkflowId

data class DecisionDispatched(
    override var decisionId: DecisionId,
    override var sentAt: DateTime? = DateTime(),
    val decisionName: DecisionName,
    val decisionData: DecisionData?,
    val workflowId: WorkflowId? = null
) : DecisionMessageInterface
