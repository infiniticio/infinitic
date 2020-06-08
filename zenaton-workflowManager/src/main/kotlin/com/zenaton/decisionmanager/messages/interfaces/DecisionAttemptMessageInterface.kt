package com.zenaton.decisionmanager.messages.interfaces

import com.zenaton.decisionmanager.data.DecisionAttemptId

interface DecisionAttemptMessageInterface : DecisionMessageInterface {
    val decisionAttemptId: DecisionAttemptId
    val decisionAttemptIndex: Int
}
