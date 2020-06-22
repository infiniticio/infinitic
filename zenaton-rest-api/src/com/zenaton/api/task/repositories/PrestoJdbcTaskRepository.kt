package com.zenaton.api.task.repositories

import com.zenaton.api.extensions.java.sql.SqlStatement
import com.zenaton.api.extensions.java.sql.query
import com.zenaton.api.task.messages.TaskMessage
import com.zenaton.api.task.messages.commands.DispatchTaskCommand
import com.zenaton.api.task.messages.events.*
import com.zenaton.api.task.models.*
import java.sql.Connection
import java.time.Instant

class PrestoJdbcTaskRepository(private val prestoConnection: Connection) : TaskRepository {
    override fun getById(id: String): Task? {
        val sqlStatement = SqlStatement("""SELECT * FROM "public/default".tasks WHERE taskid = ? ORDER BY __publish_time__ ASC""") {
            it.setString(1, id)
        }

        val messages = prestoConnection.query(sqlStatement) {
            when (TaskMessage.Type.fromString(it.getString(TaskMessage.Fields.TYPE))) {
                // Commands
                TaskMessage.Type.DISPATCH_TASK -> DispatchTaskCommand(
                    taskId = it.getString(TaskMessage.Fields.TASK_ID),
                    taskName = it.getString(DispatchTaskCommand.Fields.TASK_NAME),
                    sentAt = Instant.ofEpochMilli(it.getLong(DispatchTaskCommand.Fields.SENT_AT))
                )
                TaskMessage.Type.RETRY_TASK -> null
                TaskMessage.Type.RETRY_TASK_ATTEMPT -> null
                TaskMessage.Type.TIMEOUT_TASK_ATTEMPT -> null
                // Events
                TaskMessage.Type.TASK_ATTEMPT_COMPLETED -> TaskAttemptCompletedEvent(
                    attemptId = it.getString(TaskAttemptCompletedEvent.Fields.ATTEMPT_ID),
                    attemptIndex = it.getInt(TaskAttemptCompletedEvent.Fields.ATTEMPT_INDEX),
                    sentAt = Instant.ofEpochMilli(it.getLong(TaskMessage.Fields.SENT_AT))
                )
                TaskMessage.Type.TASK_ATTEMPT_DISPATCHED -> TaskAttemptDispatchedEvent(
                    attemptId = it.getString(TaskAttemptDispatchedEvent.Fields.ATTEMPT_ID),
                    attemptIndex = it.getInt(TaskAttemptDispatchedEvent.Fields.ATTEMPT_INDEX),
                    sentAt = Instant.ofEpochMilli(it.getLong(TaskMessage.Fields.SENT_AT))
                )
                TaskMessage.Type.TASK_ATTEMPT_FAILED -> TaskAttemptFailedEvent(
                    attemptId = it.getString(TaskAttemptFailedEvent.Fields.ATTEMPT_ID),
                    attemptIndex = it.getInt(TaskAttemptFailedEvent.Fields.ATTEMPT_INDEX),
                    sentAt = Instant.ofEpochMilli(it.getLong(TaskMessage.Fields.SENT_AT)),
                    delayBeforeRetry = it.getFloat(TaskAttemptFailedEvent.Fields.DELAY_BEFORE_RETRY)
                )
                TaskMessage.Type.TASK_ATTEMPT_STARTED -> TaskAttemptStartedEvent(
                    attemptId = it.getString(TaskAttemptStartedEvent.Fields.ATTEMPT_ID),
                    attemptIndex = it.getInt(TaskAttemptStartedEvent.Fields.ATTEMPT_INDEX),
                    sentAt = Instant.ofEpochMilli(it.getLong(TaskMessage.Fields.SENT_AT)),
                    delayBeforeRetry = it.getFloat(TaskAttemptStartedEvent.Fields.DELAY_BEFORE_RETRY),
                    delayBeforeTimeout = it.getFloat(TaskAttemptStartedEvent.Fields.DELAY_BEFORE_TIMEOUT)
                )
            }
        }.filterNotNull()

        val builder = Task.Builder()

        messages.forEach { message ->
            when (message) {
                is DispatchTaskCommand -> {
                    builder.id = message.taskId
                    builder.name = message.taskName
                    builder.dispatchedAt = message.sentAt
                }
                is TaskAttemptCompletedEvent -> {
                    builder.completedAt = builder.completedAt ?: message.sentAt
                    builder.attempts.get(message.attemptId)?.apply {
                        tries.findWithIndex(message.attemptIndex)?.completedAt = message.sentAt
                    }
                }
                is TaskAttemptDispatchedEvent -> {
                    val attempt = builder.attempts.get(message.attemptId) ?: TaskAttempt.Builder().apply {
                        this.id = message.attemptId
                    }.also {
                        builder.attempts.add(it)
                    }

                    TaskAttemptTry.Builder().apply { index = message.attemptIndex }.also { attempt.tries.add(it) }
                }
                is TaskAttemptFailedEvent -> {
                    builder.failedAt = builder.failedAt ?: message.sentAt
                    builder.attempts.get(message.attemptId)?.tries?.findWithIndex(message.attemptIndex)?.apply {
                        failedAt = message.sentAt
                        delayBeforeRetry = message.delayBeforeRetry
                    }
                }
                is TaskAttemptStartedEvent -> {
                    builder.startedAt = builder.startedAt ?: message.sentAt
                    builder.attempts.get(message.attemptId)?.tries?.findWithIndex(message.attemptIndex)?.startedAt = message.sentAt
                }
            }
        }

        return builder.build()
    }
}
