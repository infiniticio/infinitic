package com.zenaton.pulsar.topics.tasks.functions

import com.zenaton.engine.taskAttempts.messages.TaskAttemptDispatched
import com.zenaton.engine.tasks.engine.TaskEngineDispatcherInterface
import com.zenaton.engine.workflows.messages.TaskCompleted
import com.zenaton.pulsar.topics.taskAttempts.dispatcher.TaskAttemptDispatcher
import com.zenaton.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import org.apache.pulsar.functions.api.Context

class TaskEngineDispatcher(private val context: Context) : TaskEngineDispatcherInterface {

    override fun dispatch(msg: TaskAttemptDispatched, after: Float) {
        TaskAttemptDispatcher.dispatch(context, msg, after)
    }

    override fun dispatch(msg: TaskCompleted, after: Float) {
        WorkflowDispatcher.dispatch(context, msg, after)
    }
}
