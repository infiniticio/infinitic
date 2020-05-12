package com.zenaton.pulsar.topics.tasks.functions

import com.zenaton.engine.topics.taskAttempts.messages.TaskAttemptMessage
import com.zenaton.engine.topics.tasks.interfaces.TaskEngineDispatcherInterface
import com.zenaton.engine.topics.tasks.messages.TaskAttemptRetried
import com.zenaton.engine.topics.tasks.messages.TaskAttemptTimeout
import com.zenaton.engine.topics.workflows.messages.TaskCompleted
import com.zenaton.pulsar.topics.taskAttempts.dispatcher.TaskAttemptDispatcher
import com.zenaton.pulsar.topics.tasks.dispatcher.TaskMessageDispatcher
import com.zenaton.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.functions.api.Context

/**
 * This class provides a Pulsar implementation of 'TaskEngineDispatcherInterface' used in TaskEngine
 */
class TaskEngineDispatcher(private val context: Context) : TaskEngineDispatcherInterface {

    override fun dispatch(msg: TaskAttemptMessage): MessageId {
        return TaskAttemptDispatcher.dispatch(context, msg)
    }

    override fun dispatch(msg: TaskCompleted): MessageId {
        return WorkflowDispatcher.dispatch(context, msg)
    }

    override fun dispatch(msg: TaskAttemptRetried, after: Float): MessageId {
        return TaskMessageDispatcher.dispatch(context, msg, after)
    }

    override fun dispatch(msg: TaskAttemptTimeout, after: Float): MessageId {
        return TaskMessageDispatcher.dispatch(context, msg, after)
    }
}
