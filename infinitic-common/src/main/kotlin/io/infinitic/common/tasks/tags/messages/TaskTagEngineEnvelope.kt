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

package io.infinitic.common.tasks.tags.messages

import io.infinitic.common.messages.Envelope
import io.infinitic.common.serDe.avro.AvroSerDe
import kotlinx.serialization.Serializable

@Serializable
data class TaskTagEngineEnvelope(
    val name: String,
    val type: TaskTagEngineMessageType,
    val addTaskTag: AddTaskTag? = null,
    val removeTaskTag: RemoveTaskTag? = null,
    val cancelTaskPerTag: CancelTaskPerTag? = null,
    val retryTaskPerTag: RetryTaskPerTag? = null,
    val getTaskIds: GetTaskIds? = null
) : Envelope<TaskTagEngineMessage> {
    init {
        val noNull = listOfNotNull(
            addTaskTag,
            removeTaskTag,
            cancelTaskPerTag,
            retryTaskPerTag,
            getTaskIds
        )

        require(noNull.size == 1)
        require(noNull.first() == message())
        require("${noNull.first().taskName}" == name)
    }

    companion object {
        fun from(msg: TaskTagEngineMessage) = when (msg) {
            is AddTaskTag -> TaskTagEngineEnvelope(
                "${msg.taskName}",
                TaskTagEngineMessageType.ADD_TASK_TAG,
                addTaskTag = msg
            )
            is RemoveTaskTag -> TaskTagEngineEnvelope(
                "${msg.taskName}",
                TaskTagEngineMessageType.REMOVE_TASK_TAG,
                removeTaskTag = msg
            )
            is CancelTaskPerTag -> TaskTagEngineEnvelope(
                "${msg.taskName}",
                TaskTagEngineMessageType.CANCEL_TASK_PER_TAG,
                cancelTaskPerTag = msg
            )
            is RetryTaskPerTag -> TaskTagEngineEnvelope(
                "${msg.taskName}",
                TaskTagEngineMessageType.RETRY_TASK_PER_TAG,
                retryTaskPerTag = msg
            )
            is GetTaskIds -> TaskTagEngineEnvelope(
                "${msg.taskName}",
                TaskTagEngineMessageType.GET_TASK_IDS,
                getTaskIds = msg
            )
        }

        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    override fun message() = when (type) {
        TaskTagEngineMessageType.ADD_TASK_TAG -> addTaskTag!!
        TaskTagEngineMessageType.REMOVE_TASK_TAG -> removeTaskTag!!
        TaskTagEngineMessageType.CANCEL_TASK_PER_TAG -> cancelTaskPerTag!!
        TaskTagEngineMessageType.RETRY_TASK_PER_TAG -> retryTaskPerTag!!
        TaskTagEngineMessageType.GET_TASK_IDS -> getTaskIds!!
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
