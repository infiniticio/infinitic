package com.zenaton.engine.pulsar.functions.workflows

import com.zenaton.engine.pulsar.messages.Message
import com.zenaton.engine.pulsar.messages.MessageType
import com.zenaton.engine.tasks.Message.TaskDispatched
import com.zenaton.engine.workflows.Message.WorkflowDispatched
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class State : Function<Message, Void> {
    lateinit var context: Context

    override fun process(input: Message, context: Context?): Void? {
        this.context = if (context == null) throw NullPointerException() else context

        when (input.type) {
            MessageType.WORKFLOW_DISPATCHED -> handle(input.get<WorkflowDispatched>())
            MessageType.TASK_DISPATCHED -> handle(input.get<TaskDispatched>())
            else -> throw UnsupportedOperationException()
        }

        return null
    }

    private fun handle(msg: WorkflowDispatched) {
        val logMessage = String.format(
            "WorkflowDispatched: {workflowId: \"%s\", workflowName: \"%s\"}",
            msg.workflowId,
            msg.workflowName
        )
        context.logger.info(logMessage)
    }

    private fun handle(msg: TaskDispatched) {
        val logMessage = String.format(
            "TaskDispatched: {taskId: \"%s\", taskName: \"%s\"}",
            msg.taskId,
            msg.taskName
        )
        context.logger.info(logMessage)
    }
}
