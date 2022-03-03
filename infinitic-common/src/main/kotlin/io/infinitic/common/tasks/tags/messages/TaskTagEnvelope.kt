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
data class TaskTagEnvelope(
    val name: String,
    val type: TaskTagMessageType,
    val addTagToTask: AddTagToTask? = null,
    val removeTagFromTask: RemoveTagFromTask? = null,
    val cancelTaskByTag: CancelTaskByTag? = null,
    val retryTaskByTag: RetryTaskByTag? = null,
    val getTaskIdsByTag: GetTaskIdsByTag? = null
) : Envelope<TaskTagMessage> {
    init {
        val noNull = listOfNotNull(
            addTagToTask,
            removeTagFromTask,
            cancelTaskByTag,
            retryTaskByTag,
            getTaskIdsByTag
        )

        require(noNull.size == 1)
        require(noNull.first() == message())
        require("${noNull.first().taskName}" == name)
    }

    companion object {
        fun from(msg: TaskTagMessage) = when (msg) {
            is AddTagToTask -> TaskTagEnvelope(
                "${msg.taskName}",
                TaskTagMessageType.ADD_TAG_TO_TASK,
                addTagToTask = msg
            )
            is RemoveTagFromTask -> TaskTagEnvelope(
                "${msg.taskName}",
                TaskTagMessageType.REMOVE_TAG_FROM_TASK,
                removeTagFromTask = msg
            )
            is CancelTaskByTag -> TaskTagEnvelope(
                "${msg.taskName}",
                TaskTagMessageType.CANCEL_TASK_BY_TAG,
                cancelTaskByTag = msg
            )
            is RetryTaskByTag -> TaskTagEnvelope(
                "${msg.taskName}",
                TaskTagMessageType.RETRY_TASK_BY_TAG,
                retryTaskByTag = msg
            )
            is GetTaskIdsByTag -> TaskTagEnvelope(
                "${msg.taskName}",
                TaskTagMessageType.GET_TASK_IDS_BY_TAG,
                getTaskIdsByTag = msg
            )
        }

        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    override fun message() = when (type) {
        TaskTagMessageType.ADD_TAG_TO_TASK -> addTagToTask!!
        TaskTagMessageType.REMOVE_TAG_FROM_TASK -> removeTagFromTask!!
        TaskTagMessageType.CANCEL_TASK_BY_TAG -> cancelTaskByTag!!
        TaskTagMessageType.RETRY_TASK_BY_TAG -> retryTaskByTag!!
        TaskTagMessageType.GET_TASK_IDS_BY_TAG -> getTaskIdsByTag!!
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
