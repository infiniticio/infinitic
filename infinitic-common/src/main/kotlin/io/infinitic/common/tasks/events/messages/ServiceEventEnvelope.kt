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
package io.infinitic.common.tasks.events.messages

import com.github.avrokotlin.avro4k.AvroName
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.messages.Envelope
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.tasks.data.ServiceName
import kotlinx.serialization.Serializable
import org.apache.avro.Schema

@Serializable
@AvroNamespace("io.infinitic.tasks.events")
@AvroName("TaskEventEnvelope")
data class ServiceEventEnvelope(
  private val serviceName: ServiceName,
  private val type: ServiceEventMessageType,
  private val taskStartedEvent: TaskStartedEvent? = null,
  private val taskRetried: TaskRetriedEvent? = null,
  private val taskFailedEvent: TaskFailedEvent? = null,
  private val taskCompletedEvent: TaskCompletedEvent? = null,
) : Envelope<ServiceExecutorEventMessage> {
  init {
    val noNull = listOfNotNull(
        taskStartedEvent,
        taskRetried,
        taskFailedEvent,
        taskCompletedEvent,
    )

    require(noNull.size == 1)
    require(noNull.first() == message())
    require(noNull.first().serviceName == serviceName)
  }

  companion object {
    fun from(msg: ServiceExecutorEventMessage) = when (msg) {
      is TaskStartedEvent -> ServiceEventEnvelope(
          serviceName = msg.serviceName,
          type = ServiceEventMessageType.TASK_STARTED,
          taskStartedEvent = msg,
      )

      is TaskRetriedEvent -> ServiceEventEnvelope(
          serviceName = msg.serviceName,
          type = ServiceEventMessageType.TASK_RETRIED,
          taskRetried = msg,
      )

      is TaskFailedEvent -> ServiceEventEnvelope(
          serviceName = msg.serviceName,
          type = ServiceEventMessageType.TASK_FAILED,
          taskFailedEvent = msg,
      )

      is TaskCompletedEvent -> ServiceEventEnvelope(
          serviceName = msg.serviceName,
          type = ServiceEventMessageType.TASK_COMPLETED,
          taskCompletedEvent = msg,
      )
    }

    /** Deserialize from a byte array and an avro schema */
    fun fromByteArray(bytes: ByteArray, readerSchema: Schema) =
        AvroSerDe.readBinary(bytes, readerSchema, serializer())

    /** Current avro Schema */
    val writerSchema = AvroSerDe.currentSchema(serializer())
  }

  override fun message(): ServiceExecutorEventMessage =
      when (type) {
        ServiceEventMessageType.TASK_STARTED -> taskStartedEvent
        ServiceEventMessageType.TASK_RETRIED -> taskRetried
        ServiceEventMessageType.TASK_FAILED -> taskFailedEvent
        ServiceEventMessageType.TASK_COMPLETED -> taskCompletedEvent
      }!!

  fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
