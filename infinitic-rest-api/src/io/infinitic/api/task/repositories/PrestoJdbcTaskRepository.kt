package io.infinitic.api.task.repositories

import io.infinitic.api.extensions.java.sql.SqlStatement
import io.infinitic.api.extensions.java.sql.query
import io.infinitic.api.task.messages.TaskMessage
import io.infinitic.api.task.messages.commands.DispatchTaskCommand
import io.infinitic.api.task.messages.events.*
import io.infinitic.api.task.models.*
import java.sql.Connection
import java.text.SimpleDateFormat

class PrestoJdbcTaskRepository(private val prestoConnection: Connection) : TaskRepository {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z")

    override fun getById(id: String): Task? {
        val sqlStatement = SqlStatement("""SELECT *, __publish_time__ AT TIME ZONE 'UTC' AS "utc_publish_time" FROM "public/default"."tasks-engine" WHERE taskid = ? ORDER BY __publish_time__ ASC""") {
            it.setString(1, id)
        }

        val messages = prestoConnection.query(sqlStatement) {
            when (TaskMessage.Type.fromString(it.getString(TaskMessage.Fields.TYPE))) {
                // Commands
                TaskMessage.Type.DISPATCH_TASK -> DispatchTaskCommand(
                    taskId = it.getString(TaskMessage.Fields.TASK_ID),
                    taskName = it.getString(DispatchTaskCommand.Fields.JOB_NAME),
                    sentAt = dateFormat.parse(it.getString("utc_publish_time")).toInstant()
                )
                TaskMessage.Type.RETRY_TASK -> null
                TaskMessage.Type.RETRY_TASK_ATTEMPT -> null
                TaskMessage.Type.TIMEOUT_TASK_ATTEMPT -> null
                // Events
                TaskMessage.Type.TASK_ATTEMPT_COMPLETED -> TaskAttemptCompletedEvent(
                    attemptId = it.getString(TaskAttemptCompletedEvent.Fields.ATTEMPT_ID),
                    attemptRetry = it.getInt(TaskAttemptCompletedEvent.Fields.ATTEMPT_RETRY),
                    attemptIndex = it.getInt(TaskAttemptCompletedEvent.Fields.ATTEMPT_INDEX),
                    sentAt = dateFormat.parse(it.getString("utc_publish_time")).toInstant()
                )
                TaskMessage.Type.TASK_ATTEMPT_DISPATCHED -> TaskAttemptDispatchedEvent(
                    attemptId = it.getString(TaskAttemptDispatchedEvent.Fields.ATTEMPT_ID),
                    attemptRetry = it.getInt(TaskAttemptDispatchedEvent.Fields.ATTEMPT_RETRY),
                    attemptIndex = it.getInt(TaskAttemptDispatchedEvent.Fields.ATTEMPT_INDEX),
                    sentAt = dateFormat.parse(it.getString("utc_publish_time")).toInstant()
                )
                TaskMessage.Type.TASK_ATTEMPT_FAILED -> TaskAttemptFailedEvent(
                    attemptId = it.getString(TaskAttemptFailedEvent.Fields.ATTEMPT_ID),
                    attemptRetry = it.getInt(TaskAttemptFailedEvent.Fields.ATTEMPT_RETRY),
                    attemptIndex = it.getInt(TaskAttemptFailedEvent.Fields.ATTEMPT_INDEX),
                    sentAt = dateFormat.parse(it.getString("utc_publish_time")).toInstant(),
                    delayBeforeRetry = it.getFloat(TaskAttemptFailedEvent.Fields.DELAY_BEFORE_RETRY)
                )
                TaskMessage.Type.TASK_ATTEMPT_STARTED -> TaskAttemptStartedEvent(
                    attemptId = it.getString(TaskAttemptStartedEvent.Fields.ATTEMPT_ID),
                    attemptRetry = it.getInt(TaskAttemptStartedEvent.Fields.ATTEMPT_RETRY),
                    attemptIndex = it.getInt(TaskAttemptStartedEvent.Fields.ATTEMPT_INDEX),
                    sentAt = dateFormat.parse(it.getString("utc_publish_time")).toInstant()
                )
                TaskMessage.Type.TASK_COMPLETED -> Unit
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
                    val attempt = builder.attempts.get(message.attemptId) ?: TaskAttempt.Builder().apply {
                        this.id = message.attemptId
                        this.index = message.attemptIndex
                    }.also {
                        builder.attempts.add(it)
                    }

                    val attemptTry = attempt.tries.findWithRetry(message.attemptRetry) ?: TaskAttemptTry.Builder().apply {
                        this.retry = message.attemptRetry
                    }.also {
                        attempt.tries.add(it)
                    }

                    builder.completedAt = builder.completedAt ?: message.sentAt
                    attemptTry.completedAt = message.sentAt
                }
                is TaskAttemptDispatchedEvent -> {
                    val attempt = builder.attempts.get(message.attemptId) ?: TaskAttempt.Builder().apply {
                        this.id = message.attemptId
                        this.index = message.attemptIndex
                    }.also {
                        builder.attempts.add(it)
                    }

                    val attemptTry = attempt.tries.findWithRetry(message.attemptRetry) ?: TaskAttemptTry.Builder().apply {
                        this.retry = message.attemptRetry
                    }.also {
                        attempt.tries.add(it)
                    }

                    attemptTry.dispatchedAt = message.sentAt
                }
                is TaskAttemptFailedEvent -> {
                    val attempt = builder.attempts.get(message.attemptId) ?: TaskAttempt.Builder().apply {
                        this.id = message.attemptId
                        this.index = message.attemptIndex
                    }.also {
                        builder.attempts.add(it)
                    }

                    val attemptTry = attempt.tries.findWithRetry(message.attemptRetry) ?: TaskAttemptTry.Builder().apply {
                        this.retry = message.attemptRetry
                    }.also {
                        attempt.tries.add(it)
                    }

                    builder.failedAt = message.sentAt
                    attemptTry.failedAt = message.sentAt
                    attemptTry.delayBeforeRetry = message.delayBeforeRetry
                }
                is TaskAttemptStartedEvent -> {
                    val attempt = builder.attempts.get(message.attemptId) ?: TaskAttempt.Builder().apply {
                        this.id = message.attemptId
                        this.index = message.attemptIndex
                    }.also {
                        builder.attempts.add(it)
                    }

                    val attemptTry = attempt.tries.findWithRetry(message.attemptRetry) ?: TaskAttemptTry.Builder().apply {
                        this.retry = message.attemptRetry
                    }.also {
                        attempt.tries.add(it)
                    }

                    builder.startedAt = builder.startedAt ?: message.sentAt
                    attemptTry.startedAt = message.sentAt
                }
            }
        }

        return builder.build()
    }
}
