package com.zenaton.api.task.repositories

import com.zenaton.api.extensions.java.sql.SqlStatement
import com.zenaton.api.extensions.java.sql.query
import com.zenaton.api.task.messages.JobMessage
import com.zenaton.api.task.messages.commands.DispatchJobCommand
import com.zenaton.api.task.messages.events.*
import com.zenaton.api.task.models.*
import java.sql.Connection
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PrestoJdbcTaskRepository(private val prestoConnection: Connection) : TaskRepository {
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    override fun getById(id: String): Task? {
        val sqlStatement = SqlStatement("""SELECT * FROM "public/default"."tasks-engine" WHERE jobid = ? ORDER BY __publish_time__ ASC""") {
            it.setString(1, id)
        }

        val messages = prestoConnection.query(sqlStatement) {
            when (JobMessage.Type.fromString(it.getString(JobMessage.Fields.TYPE))) {
                // Commands
                JobMessage.Type.DISPATCH_JOB -> DispatchJobCommand(
                    jobId = it.getString(JobMessage.Fields.JOB_ID),
                    jobName = it.getString(DispatchJobCommand.Fields.JOB_NAME),
                    sentAt = LocalDateTime.parse(it.getString(JobMessage.Fields.SENT_AT), dateTimeFormatter).atZone(ZoneId.of("UTC")).toInstant()
                )
                JobMessage.Type.RETRY_JOB -> null
                JobMessage.Type.RETRY_JOB_ATTEMPT -> null
                JobMessage.Type.TIMEOUT_JOB_ATTEMPT -> null
                // Events
                JobMessage.Type.JOB_ATTEMPT_COMPLETED -> JobAttemptCompletedEvent(
                    attemptId = it.getString(JobAttemptCompletedEvent.Fields.ATTEMPT_ID),
                    attemptRetry = it.getInt(JobAttemptCompletedEvent.Fields.ATTEMPT_RETRY),
                    attemptIndex = it.getInt(JobAttemptCompletedEvent.Fields.ATTEMPT_INDEX),
                    sentAt = LocalDateTime.parse(it.getString(JobMessage.Fields.SENT_AT), dateTimeFormatter).atZone(ZoneId.of("UTC")).toInstant()
                )
                JobMessage.Type.JOB_ATTEMPT_DISPATCHED -> JobAttemptDispatchedEvent(
                    attemptId = it.getString(JobAttemptDispatchedEvent.Fields.ATTEMPT_ID),
                    attemptRetry = it.getInt(JobAttemptDispatchedEvent.Fields.ATTEMPT_RETRY),
                    attemptIndex = it.getInt(JobAttemptDispatchedEvent.Fields.ATTEMPT_INDEX),
                    sentAt = LocalDateTime.parse(it.getString(JobMessage.Fields.SENT_AT), dateTimeFormatter).atZone(ZoneId.of("UTC")).toInstant()
                )
                JobMessage.Type.JOB_ATTEMPT_FAILED -> JobAttemptFailedEvent(
                    attemptId = it.getString(JobAttemptFailedEvent.Fields.ATTEMPT_ID),
                    attemptRetry = it.getInt(JobAttemptFailedEvent.Fields.ATTEMPT_RETRY),
                    attemptIndex = it.getInt(JobAttemptFailedEvent.Fields.ATTEMPT_INDEX),
                    sentAt = LocalDateTime.parse(it.getString(JobMessage.Fields.SENT_AT), dateTimeFormatter).atZone(ZoneId.of("UTC")).toInstant(),
                    delayBeforeRetry = it.getFloat(JobAttemptFailedEvent.Fields.DELAY_BEFORE_RETRY)
                )
                JobMessage.Type.JOB_ATTEMPT_STARTED -> JobAttemptStartedEvent(
                    attemptId = it.getString(JobAttemptStartedEvent.Fields.ATTEMPT_ID),
                    attemptRetry = it.getInt(JobAttemptStartedEvent.Fields.ATTEMPT_RETRY),
                    attemptIndex = it.getInt(JobAttemptStartedEvent.Fields.ATTEMPT_INDEX),
                    sentAt = LocalDateTime.parse(it.getString(JobMessage.Fields.SENT_AT), dateTimeFormatter).atZone(ZoneId.of("UTC")).toInstant()
                )
                JobMessage.Type.JOB_COMPLETED -> Unit
            }
        }.filterNotNull()

        val builder = Task.Builder()

        messages.forEach { message ->
            when (message) {
                is DispatchJobCommand -> {
                    builder.id = message.jobId
                    builder.name = message.jobName
                    builder.dispatchedAt = message.sentAt
                }
                is JobAttemptCompletedEvent -> {
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
                is JobAttemptDispatchedEvent -> {
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
                is JobAttemptFailedEvent -> {
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
                is JobAttemptStartedEvent -> {
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
