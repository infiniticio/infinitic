package com.zenaton.engine.decisionAttempts.messages

import com.zenaton.engine.decisionAttempts.data.DecisionAttemptId
import com.zenaton.engine.decisions.data.DecisionId
import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.workflows.data.WorkflowData
import com.zenaton.engine.workflows.data.WorkflowName

data class DecisionAttemptDispatched(
    override var decisionId: DecisionId,
    override val decisionAttemptId: DecisionAttemptId,
    override val decisionAttemptIndex: Int,
    override var sentAt: DateTime? = DateTime(),
    override var receivedAt: DateTime? = null,
    val workflowName: WorkflowName,
    val workflowData: WorkflowData
) : DecisionAttemptMessageInterface {
    override fun getName() = workflowName
}
