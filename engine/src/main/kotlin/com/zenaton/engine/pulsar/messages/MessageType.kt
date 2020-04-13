package com.zenaton.engine.pulsar.messages

import com.zenaton.engine.tasks.Message.TaskDispatched
import com.zenaton.engine.workflows.Message.WorkflowDispatched

enum class MessageType(val className: String?) {
    WORKFLOW_DISPATCHED(WorkflowDispatched::class.qualifiedName),
    TASK_DISPATCHED(TaskDispatched::class.qualifiedName)
}
