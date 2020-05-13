package com.zenaton.taskmanager.pulsar.functions

import com.zenaton.taskmanager.engine.TaskEngineDispatcherInterface
import com.zenaton.taskmanager.messages.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.TaskAttemptRetried
import com.zenaton.taskmanager.messages.TaskAttemptTimeout
import com.zenaton.taskmanager.pulsar.dispatcher.TaskDispatcher
import com.zenaton.workflowengine.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import com.zenaton.workflowengine.topics.workflows.messages.TaskCompleted
import org.apache.pulsar.functions.api.Context

/**
 * This class provides a Pulsar implementation of 'TaskEngineDispatcherInterface' used in TaskEngine
 */
class TaskEngineDispatcher(private val context: Context) : TaskEngineDispatcherInterface {

    override fun dispatch(msg: TaskAttemptDispatched) {
        TaskDispatcher.dispatch(context, msg)
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
