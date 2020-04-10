package com.zenaton.engine.workflows.messages

class WorkflowDispatched(
    val workflowId: String,
    val workflowName: String
) : MessageInterface
