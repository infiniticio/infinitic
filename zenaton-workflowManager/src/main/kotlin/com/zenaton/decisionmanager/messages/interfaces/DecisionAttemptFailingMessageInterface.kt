package com.zenaton.decisionmanager.messages.interfaces

interface DecisionAttemptFailingMessageInterface : DecisionAttemptMessageInterface {
    val decisionAttemptDelayBeforeRetry: Float
}
