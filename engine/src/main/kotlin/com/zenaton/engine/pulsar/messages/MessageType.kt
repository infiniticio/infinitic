package com.zenaton.engine.pulsar.messages

import com.zenaton.engine.tasks.messages.TaskDispatched
import com.zenaton.engine.workflows.messages.WorkflowDispatched

enum class MessageType(val className: String?) {
    WORKFLOW_DISPATCHED(WorkflowDispatched::class.qualifiedName),
    TASK_DISPATCHED(TaskDispatched::class.qualifiedName)
}
