/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.common.tasks.tags.messages

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroDefault
import com.github.avrokotlin.avro4k.AvroName
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.messages.Envelope
import io.infinitic.common.serDe.avro.AvroSerDe
import kotlinx.serialization.Serializable
import org.apache.avro.Schema

@Serializable
@AvroNamespace("io.infinitic.tasks.tag")
@AvroName("TaskTagEnvelope")
data class ServiceTagEnvelope(
  private val name: String,
  @AvroNamespace("io.infinitic.tasks.tag") private val type: TaskTagMessageType,
  @AvroName("addTagToTask") private val addTaskIdToTag: AddTaskIdToTag? = null,
  @AvroName("removeTagFromTask") private val removeTaskIdFromTag: RemoveTaskIdFromTag? = null,
  private val cancelTaskByTag: CancelTaskByTag? = null,
  @Deprecated("unused") private val retryTaskByTag: RetryTaskByTag? = null,
  private val getTaskIdsByTag: GetTaskIdsByTag? = null,
  @AvroDefault(Avro.NULL) private val setDelegatedTaskData: SetDelegatedTaskData? = null,
  @AvroDefault(Avro.NULL) private val completeDelegatedTask: CompleteDelegatedTask? = null
) : Envelope<ServiceTagMessage> {
  init {
    val noNull = listOfNotNull(
        addTaskIdToTag,
        removeTaskIdFromTag,
        cancelTaskByTag,
        retryTaskByTag,
        getTaskIdsByTag,
        setDelegatedTaskData,
        completeDelegatedTask,
    )

    require(noNull.size == 1)
    require(noNull.first() == message())
    require("${noNull.first().serviceName}" == name)
  }

  companion object {
    fun from(msg: ServiceTagMessage) = when (msg) {
      is AddTaskIdToTag -> ServiceTagEnvelope(
          name = "${msg.serviceName}",
          type = TaskTagMessageType.ADD_TAG_TO_TASK,
          addTaskIdToTag = msg,
      )

      is RemoveTaskIdFromTag -> ServiceTagEnvelope(
          name = "${msg.serviceName}",
          type = TaskTagMessageType.REMOVE_TAG_FROM_TASK,
          removeTaskIdFromTag = msg,
      )

      is CancelTaskByTag -> ServiceTagEnvelope(
          name = "${msg.serviceName}",
          type = TaskTagMessageType.CANCEL_TASK_BY_TAG,
          cancelTaskByTag = msg,
      )

      is RetryTaskByTag -> ServiceTagEnvelope(
          name = "${msg.serviceName}",
          type = TaskTagMessageType.RETRY_TASK_BY_TAG,
          retryTaskByTag = msg,
      )

      is GetTaskIdsByTag -> ServiceTagEnvelope(
          name = "${msg.serviceName}",
          type = TaskTagMessageType.GET_TASK_IDS_BY_TAG,
          getTaskIdsByTag = msg,
      )

      is SetDelegatedTaskData -> ServiceTagEnvelope(
          name = "${msg.serviceName}",
          type = TaskTagMessageType.SET_ASYNC_TASK_DATA,
          setDelegatedTaskData = msg,
      )

      is CompleteDelegatedTask -> ServiceTagEnvelope(
          name = "${msg.serviceName}",
          type = TaskTagMessageType.COMPLETE_ASYNC_TASK,
          completeDelegatedTask = msg,
      )

      else -> thisShouldNotHappen()
    }

    /** Deserialize from a byte array and an avro schema */
    fun fromByteArray(bytes: ByteArray, readerSchema: Schema) =
        AvroSerDe.readBinary(bytes, readerSchema, serializer())

    /** Current avro Schema */
    val writerSchema = AvroSerDe.currentSchema(serializer())
  }

  override fun message() =
      when (type) {
        TaskTagMessageType.ADD_TAG_TO_TASK -> addTaskIdToTag
        TaskTagMessageType.REMOVE_TAG_FROM_TASK -> removeTaskIdFromTag
        TaskTagMessageType.CANCEL_TASK_BY_TAG -> cancelTaskByTag
        TaskTagMessageType.RETRY_TASK_BY_TAG -> retryTaskByTag
        TaskTagMessageType.GET_TASK_IDS_BY_TAG -> getTaskIdsByTag
        TaskTagMessageType.SET_ASYNC_TASK_DATA -> setDelegatedTaskData
        TaskTagMessageType.COMPLETE_ASYNC_TASK -> completeDelegatedTask
      }!!

  fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
