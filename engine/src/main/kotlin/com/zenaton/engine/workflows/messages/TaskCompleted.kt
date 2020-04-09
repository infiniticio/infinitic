package com.zenaton.engine.workflows.messages

class TaskCompleted(
    val workflowId: String,
    val taskId: String
) : MessageInterface
