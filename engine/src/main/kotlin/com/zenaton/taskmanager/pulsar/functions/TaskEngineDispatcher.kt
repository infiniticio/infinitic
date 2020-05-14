package com.zenaton.taskmanager.pulsar.functions

import com.zenaton.taskmanager.engine.TaskEngineDispatcherInterface
import com.zenaton.taskmanager.messages.RunTask
import com.zenaton.taskmanager.messages.commands.RetryTaskAttempt
import com.zenaton.taskmanager.messages.commands.TimeOutTaskAttempt
import com.zenaton.taskmanager.messages.events.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.events.TaskAttemptTimedOut
import com.zenaton.taskmanager.pulsar.dispatcher.TaskDispatcher
import com.zenaton.workflowengine.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import com.zenaton.workflowengine.topics.workflows.messages.TaskCompleted
import org.apache.pulsar.functions.api.Context

/**
 * This class provides a Pulsar implementation of 'TaskEngineDispatcherInterface' used in TaskEngine
 */
class TaskEngineDispatcher(private val context: Context) : TaskEngineDispatcherInterface {

    override fun dispatch(msg: RunTask) {
        TaskDispatcher.dispatch(context, msg)
    }

    override fun dispatch(msg: TaskAttemptTimedOut) {
        TaskDispatcher.dispatch(context, msg)
    }

    override fun dispatch(msg: TaskAttemptDispatched) {
        TaskDispatcher.dispatch(context, msg)
    }

    override fun dispatch(msg: TaskCompleted) {
        WorkflowDispatcher.dispatch(context, msg)
    }

    override fun dispatch(msg: RetryTaskAttempt, after: Float) {
        TaskDispatcher.dispatch(context, msg, after)
    }

    override fun dispatch(msg: TimeOutTaskAttempt, after: Float) {
        TaskDispatcher.dispatch(context, msg, after)
    }
}
