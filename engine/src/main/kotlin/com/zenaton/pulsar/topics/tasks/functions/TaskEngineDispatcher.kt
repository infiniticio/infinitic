package com.zenaton.pulsar.topics.tasks.functions

import com.zenaton.engine.taskAttempts.messages.TaskAttemptDispatched
import com.zenaton.engine.tasks.interfaces.TaskEngineDispatcherInterface
import com.zenaton.engine.tasks.messages.TaskAttemptRetried
import com.zenaton.engine.tasks.messages.TaskAttemptTimeout
import com.zenaton.engine.workflows.messages.TaskCompleted
import com.zenaton.pulsar.topics.taskAttempts.dispatcher.TaskAttemptDispatcher
import com.zenaton.pulsar.topics.tasks.dispatcher.TaskDispatcher
import com.zenaton.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import org.apache.pulsar.functions.api.Context

class TaskEngineDispatcher(private val context: Context) :
    TaskEngineDispatcherInterface {

    override fun dispatch(msg: TaskAttemptDispatched) {
        TaskAttemptDispatcher.dispatch(context, msg)
    }

    override fun dispatch(msg: TaskCompleted) {
        WorkflowDispatcher.dispatch(context, msg)
    }

    override fun dispatch(msg: TaskAttemptRetried, after: Float) {
        TaskDispatcher.dispatch(context, msg, after)
    }

    override fun dispatch(msg: TaskAttemptTimeout, after: Float) {
        TaskDispatcher.dispatch(context, msg, after)
    }
}
