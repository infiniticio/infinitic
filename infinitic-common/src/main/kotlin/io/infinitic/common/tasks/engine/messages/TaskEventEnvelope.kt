/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.tasks.engine.messages

import com.github.avrokotlin.avro4k.Avro
import io.infinitic.common.tasks.data.TaskId
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer

@Serializable
data class TaskEventEnvelope(
    val taskId: TaskId,
    val type: TaskEventMessageType,
    val dispatchTask: DispatchTask? = null,
    val retryTask: RetryTask? = null,
    val retryTaskAttempt: RetryTaskAttempt? = null,
    val cancelTask: CancelTask? = null,
    val taskCanceled: TaskCanceled? = null,
    val taskCompleted: TaskCompleted? = null,
    val taskAttemptDispatched: TaskAttemptDispatched? = null,
    val taskAttemptCompleted: TaskAttemptCompleted? = null,
    val taskAttemptFailed: TaskAttemptFailed? = null,
    val taskAttemptStarted: TaskAttemptStarted? = null
) {
    init {
        val notNull = listOfNotNull(
            dispatchTask,
            retryTask,
            retryTaskAttempt,
            cancelTask,
            taskCanceled,
            taskCompleted,
            taskAttemptDispatched,
            taskAttemptCompleted,
            taskAttemptFailed,
            taskAttemptStarted
        )

        require(notNull.size == 1)
        require(notNull.first() == event())
        require(notNull.first().taskId == taskId)
    }

    companion object {
        fun from(event: TaskEventMessage) = when (event) {
            is DispatchTask -> TaskEventEnvelope(
                event.taskId,
                TaskEventMessageType.DISPATCH_TASK,
                dispatchTask = event
            )
            is RetryTask -> TaskEventEnvelope(
                event.taskId,
                TaskEventMessageType.RETRY_TASK,
                retryTask = event
            )
            is RetryTaskAttempt -> TaskEventEnvelope(
                event.taskId,
                TaskEventMessageType.RETRY_TASK_ATTEMPT,
                retryTaskAttempt = event
            )
            is CancelTask -> TaskEventEnvelope(
                event.taskId,
                TaskEventMessageType.CANCEL_TASK,
                cancelTask = event
            )
            is TaskCanceled -> TaskEventEnvelope(
                event.taskId,
                TaskEventMessageType.TASK_CANCELED,
                taskCanceled = event
            )
            is TaskCompleted -> TaskEventEnvelope(
                event.taskId,
                TaskEventMessageType.TASK_COMPLETED,
                taskCompleted = event
            )
            is TaskAttemptDispatched -> TaskEventEnvelope(
                event.taskId,
                TaskEventMessageType.TASK_ATTEMPT_DISPATCHED,
                taskAttemptDispatched = event
            )
            is TaskAttemptCompleted -> TaskEventEnvelope(
                event.taskId,
                TaskEventMessageType.TASK_ATTEMPT_COMPLETED,
                taskAttemptCompleted = event
            )
            is TaskAttemptFailed -> TaskEventEnvelope(
                event.taskId,
                TaskEventMessageType.TASK_ATTEMPT_FAILED,
                taskAttemptFailed = event
            )
            is TaskAttemptStarted -> TaskEventEnvelope(
                event.taskId,
                TaskEventMessageType.TASK_ATTEMPT_STARTED,
                taskAttemptStarted = event
            )
            else -> throw RuntimeException("This should not happen: unknown event $event")
        }

        fun fromByteArray(bytes: ByteArray) = Avro.default.decodeFromByteArray(serializer(), bytes)
        fun fromByteBuffer(bytes: ByteBuffer) = fromByteArray(bytes.array())
    }

    fun event(): TaskEventMessage = when (type) {
        TaskEventMessageType.DISPATCH_TASK -> dispatchTask!!
        TaskEventMessageType.RETRY_TASK -> retryTask!!
        TaskEventMessageType.RETRY_TASK_ATTEMPT -> retryTaskAttempt!!
        TaskEventMessageType.CANCEL_TASK -> cancelTask!!
        TaskEventMessageType.TASK_CANCELED -> taskCanceled!!
        TaskEventMessageType.TASK_COMPLETED -> taskCompleted!!
        TaskEventMessageType.TASK_ATTEMPT_DISPATCHED -> taskAttemptDispatched!!
        TaskEventMessageType.TASK_ATTEMPT_COMPLETED -> taskAttemptCompleted!!
        TaskEventMessageType.TASK_ATTEMPT_FAILED -> taskAttemptFailed!!
        TaskEventMessageType.TASK_ATTEMPT_STARTED -> taskAttemptStarted!!
    }

    fun toByteArray() = Avro.default.encodeToByteArray(serializer(), this)
    fun toByteBuffer(): ByteBuffer = ByteBuffer.wrap(toByteArray())
}
