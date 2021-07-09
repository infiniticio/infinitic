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

import io.infinitic.common.messages.Envelope
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.tasks.data.TaskId
import kotlinx.serialization.Serializable

@Serializable
data class TaskEngineEnvelope(
    val taskId: TaskId,
    val type: TaskEngineMessageType,
    val dispatchTask: DispatchTask? = null,
    val waitTask: WaitTask? = null,
    val retryTask: RetryTask? = null,
    val retryTaskAttempt: RetryTaskAttempt? = null,
    val cancelTask: CancelTask? = null,
    val completeTask: CompleteTask? = null,
    val taskAttemptCompleted: TaskAttemptCompleted? = null,
    val taskAttemptFailed: TaskAttemptFailed? = null
) : Envelope<TaskEngineMessage> {
    init {
        val noNull = listOfNotNull(
            dispatchTask,
            waitTask,
            retryTask,
            retryTaskAttempt,
            cancelTask,
            completeTask,
            taskAttemptCompleted,
            taskAttemptFailed
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
            is WaitTask -> TaskEngineEnvelope(
                msg.taskId,
                TaskEngineMessageType.WAIT_TASK,
                waitTask = msg
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
            is CompleteTask -> TaskEngineEnvelope(
                msg.taskId,
                TaskEngineMessageType.COMPLETE_TASK,
                completeTask = msg
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
        }

        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    override fun message(): TaskEngineMessage = when (type) {
        TaskEngineMessageType.DISPATCH_TASK -> dispatchTask!!
        TaskEngineMessageType.WAIT_TASK -> waitTask!!
        TaskEngineMessageType.RETRY_TASK -> retryTask!!
        TaskEngineMessageType.RETRY_TASK_ATTEMPT -> retryTaskAttempt!!
        TaskEngineMessageType.CANCEL_TASK -> cancelTask!!
        TaskEngineMessageType.COMPLETE_TASK -> completeTask!!
        TaskEngineMessageType.TASK_ATTEMPT_COMPLETED -> taskAttemptCompleted!!
        TaskEngineMessageType.TASK_ATTEMPT_FAILED -> taskAttemptFailed!!
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
