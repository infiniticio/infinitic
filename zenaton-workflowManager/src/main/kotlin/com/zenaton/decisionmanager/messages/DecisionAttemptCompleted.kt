package com.zenaton.decisionmanager.messages

import com.zenaton.decisionmanager.data.DecisionAttemptId
import com.zenaton.decisionmanager.data.DecisionId
import com.zenaton.decisionmanager.data.DecisionOutput
import com.zenaton.decisionmanager.messages.interfaces.DecisionMessageInterface

data class DecisionAttemptCompleted(
    override var decisionId: DecisionId,
    val decisionAttemptId: DecisionAttemptId,
    val decisionAttemptIndex: Int,
    val decisionOutput: DecisionOutput
) : DecisionMessageInterface
