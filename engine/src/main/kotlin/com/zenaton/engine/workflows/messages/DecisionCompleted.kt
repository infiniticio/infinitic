package com.zenaton.engine.workflows.messages

class DecisionCompleted(
    val workflowId: String,
    val decisionId: String
) : MessageInterface
