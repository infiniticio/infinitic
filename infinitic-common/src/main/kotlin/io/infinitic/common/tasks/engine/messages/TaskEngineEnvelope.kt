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
data class TaskEngineEnvelope(
    val taskId: TaskId,
    val type: TaskEngineMessageType,
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
        val noNull = listOfNotNull(
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

        require(noNull.size == 1)
        require(noNull.first() == message())
        require(noNull.first().taskId == taskId)
    }

    companion object {
        fun from(msg: TaskEngineMessage) = when (msg) {
            is DispatchTask -> TaskEngineEnvelope(
                msg.taskId,
                TaskEngineMessageType.DISPATCH_TASK,
                dispatchTask = msg
            )
            is RetryTask -> TaskEngineEnvelope(
                msg.taskId,
                TaskEngineMessageType.RETRY_TASK,
                retryTask = msg
            )
            is RetryTaskAttempt -> TaskEngineEnvelope(
                msg.taskId,
                TaskEngineMessageType.RETRY_TASK_ATTEMPT,
                retryTaskAttempt = msg
            )
            is CancelTask -> TaskEngineEnvelope(
                msg.taskId,
                TaskEngineMessageType.CANCEL_TASK,
                cancelTask = msg
            )
            is TaskCanceled -> TaskEngineEnvelope(
                msg.taskId,
                TaskEngineMessageType.TASK_CANCELED,
                taskCanceled = msg
            )
            is TaskCompleted -> TaskEngineEnvelope(
                msg.taskId,
                TaskEngineMessageType.TASK_COMPLETED,
                taskCompleted = msg
            )
            is TaskAttemptDispatched -> TaskEngineEnvelope(
                msg.taskId,
                TaskEngineMessageType.TASK_ATTEMPT_DISPATCHED,
                taskAttemptDispatched = msg
            )
            is TaskAttemptCompleted -> TaskEngineEnvelope(
                msg.taskId,
                TaskEngineMessageType.TASK_ATTEMPT_COMPLETED,
                taskAttemptCompleted = msg
            )
            is TaskAttemptFailed -> TaskEngineEnvelope(
                msg.taskId,
                TaskEngineMessageType.TASK_ATTEMPT_FAILED,
                taskAttemptFailed = msg
            )
            is TaskAttemptStarted -> TaskEngineEnvelope(
                msg.taskId,
                TaskEngineMessageType.TASK_ATTEMPT_STARTED,
                taskAttemptStarted = msg
            )
        }

        fun fromByteArray(bytes: ByteArray) = Avro.default.decodeFromByteArray(serializer(), bytes)
        fun fromByteBuffer(bytes: ByteBuffer) = fromByteArray(bytes.array())
    }

    fun message(): TaskEngineMessage = when (type) {
        TaskEngineMessageType.DISPATCH_TASK -> dispatchTask!!
        TaskEngineMessageType.RETRY_TASK -> retryTask!!
        TaskEngineMessageType.RETRY_TASK_ATTEMPT -> retryTaskAttempt!!
        TaskEngineMessageType.CANCEL_TASK -> cancelTask!!
        TaskEngineMessageType.TASK_CANCELED -> taskCanceled!!
        TaskEngineMessageType.TASK_COMPLETED -> taskCompleted!!
        TaskEngineMessageType.TASK_ATTEMPT_DISPATCHED -> taskAttemptDispatched!!
        TaskEngineMessageType.TASK_ATTEMPT_COMPLETED -> taskAttemptCompleted!!
        TaskEngineMessageType.TASK_ATTEMPT_FAILED -> taskAttemptFailed!!
        TaskEngineMessageType.TASK_ATTEMPT_STARTED -> taskAttemptStarted!!
    }

    fun toByteArray() = Avro.default.encodeToByteArray(serializer(), this)
    fun toByteBuffer(): ByteBuffer = ByteBuffer.wrap(toByteArray())
}
