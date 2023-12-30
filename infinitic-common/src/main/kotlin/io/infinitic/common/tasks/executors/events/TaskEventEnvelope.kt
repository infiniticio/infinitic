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
package io.infinitic.common.tasks.executors.events

import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.messages.Envelope
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.tasks.data.ServiceName
import kotlinx.serialization.Serializable
import org.apache.avro.Schema

@Serializable
@AvroNamespace("io.infinitic.tasks.events")
data class TaskEventEnvelope(
  private val serviceName: ServiceName,
  private val type: TaskEventMessageType,
  private val taskStarted: TaskStarted? = null,
  private val taskRetried: TaskRetried? = null,
  private val taskFailed: TaskFailed? = null,
  private val taskCompleted: TaskCompleted? = null,
) : Envelope<TaskEventMessage> {
  init {
    val noNull = listOfNotNull(
        taskStarted,
        taskRetried,
        taskFailed,
        taskCompleted,
    )

    require(noNull.size == 1)
    require(noNull.first() == message())
    require(noNull.first().serviceName == serviceName)
  }

  companion object {
    fun from(msg: TaskEventMessage) = when (msg) {
      is TaskStarted -> TaskEventEnvelope(
          serviceName = msg.serviceName,
          type = TaskEventMessageType.TASK_STARTED,
          taskStarted = msg,
      )

      is TaskRetried -> TaskEventEnvelope(
          serviceName = msg.serviceName,
          type = TaskEventMessageType.TASK_RETRIED,
          taskRetried = msg,
      )

      is TaskFailed -> TaskEventEnvelope(
          serviceName = msg.serviceName,
          type = TaskEventMessageType.TASK_FAILED,
          taskFailed = msg,
      )

      is TaskCompleted -> TaskEventEnvelope(
          serviceName = msg.serviceName,
          type = TaskEventMessageType.TASK_COMPLETED,
          taskCompleted = msg,
      )
    }

    /** Deserialize from a byte array and an avro schema */
    fun fromByteArray(bytes: ByteArray, readerSchema: Schema) =
        AvroSerDe.readBinary(bytes, readerSchema, serializer())

    /** Current avro Schema */
    val writerSchema = AvroSerDe.currentSchema(serializer())
  }

  override fun message(): TaskEventMessage =
      when (type) {
        TaskEventMessageType.TASK_STARTED -> taskStarted
        TaskEventMessageType.TASK_RETRIED -> taskRetried
        TaskEventMessageType.TASK_FAILED -> taskFailed
        TaskEventMessageType.TASK_COMPLETED -> taskCompleted
      }!!

  fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
