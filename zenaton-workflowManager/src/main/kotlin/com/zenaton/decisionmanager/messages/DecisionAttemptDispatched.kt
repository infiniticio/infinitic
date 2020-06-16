package com.zenaton.decisionmanager.messages

import com.zenaton.decisionmanager.data.DecisionAttemptId
import com.zenaton.decisionmanager.data.DecisionData
import com.zenaton.decisionmanager.data.DecisionId
import com.zenaton.decisionmanager.data.DecisionName
import com.zenaton.decisionmanager.messages.interfaces.DecisionAttemptMessageInterface

data class DecisionAttemptDispatched(
    override var decisionId: DecisionId,
    override val decisionAttemptId: DecisionAttemptId,
    override val decisionAttemptIndex: Int,
    val decisionName: DecisionName,
    val decisionData: DecisionData?
) : DecisionAttemptMessageInterface
