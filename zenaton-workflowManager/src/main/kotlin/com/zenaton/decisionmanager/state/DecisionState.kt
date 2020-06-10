package com.zenaton.decisionmanager.state

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.decisionmanager.data.DecisionAttemptId
import com.zenaton.decisionmanager.data.DecisionData
import com.zenaton.decisionmanager.data.DecisionId
import com.zenaton.decisionmanager.data.DecisionName
import com.zenaton.workflowManager.data.WorkflowId

data class DecisionState(
    val decisionId: DecisionId,
    val decisionName: DecisionName,
    val decisionData: DecisionData? = null,
    var decisionAttemptId: DecisionAttemptId = DecisionAttemptId(),
    var decisionAttemptIndex: Int = 0,
    val workflowId: WorkflowId? = null
) : StateInterface
