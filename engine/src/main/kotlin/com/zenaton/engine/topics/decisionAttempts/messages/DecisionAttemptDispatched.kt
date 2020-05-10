package com.zenaton.engine.topics.decisionAttempts.messages

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.DecisionAttemptId
import com.zenaton.engine.data.DecisionId
import com.zenaton.engine.data.WorkflowData
import com.zenaton.engine.data.WorkflowName

data class DecisionAttemptDispatched(
    override var decisionId: DecisionId,
    override val decisionAttemptId: DecisionAttemptId,
    override val decisionAttemptIndex: Int,
    override var sentAt: DateTime? = DateTime(),
    val workflowName: WorkflowName,
    val workflowData: WorkflowData
) : DecisionAttemptMessageInterface {
    @JsonIgnore
    override fun getName() = workflowName.name
}
