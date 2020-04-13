package com.zenaton.engine.tasks

import com.zenaton.engine.common.attributes.TaskAttemptId
import com.zenaton.engine.common.attributes.TaskId
import com.zenaton.engine.common.attributes.WorkflowId

sealed class Message() {
    abstract val taskId: TaskId

    data class TaskDispatched(
        override val taskId: TaskId,
        val taskName: String,
        val workflowId: WorkflowId?
    ) : Message()

    data class TaskCompleted(
        override val taskId: TaskId
    ) : Message()

    data class TaskAttemptDispatched(
        override val taskId: TaskId,
        val attemptId: TaskAttemptId
    ) : Message()

    data class TaskAttemptStarted(
        override val taskId: TaskId,
        val attemptId: TaskAttemptId
    ) : Message()

    data class TaskAttemptCompleted(
        override val taskId: TaskId,
        val attemptId: TaskAttemptId
    ) : Message()

    data class TaskAttemptFailed(
        override val taskId: TaskId,
        val attemptId: TaskAttemptId
    ) : Message()
}
