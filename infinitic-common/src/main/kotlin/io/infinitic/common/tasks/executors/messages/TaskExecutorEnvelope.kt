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
package io.infinitic.common.tasks.executors.messages

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroDefault
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.messages.Envelope
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.tasks.data.ServiceName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.apache.avro.Schema

@Serializable
@AvroNamespace("io.infinitic.tasks.executor")
data class TaskExecutorEnvelope(
  @SerialName("taskName") private val serviceName: ServiceName,
  @AvroNamespace("io.infinitic.tasks.executor") private val type: TaskExecutorMessageType,
  private val executeTask: ExecuteTask? = null,
  @AvroDefault(Avro.NULL) private val taskStarted: TaskStarted? = null,
  @AvroDefault(Avro.NULL) private val taskRetried: TaskRetried? = null,
  @AvroDefault(Avro.NULL) private val taskFailed: TaskFailed? = null,
  @AvroDefault(Avro.NULL) private val taskCompleted: TaskCompleted? = null,
) : Envelope<TaskExecutorMessage> {
  init {
    val noNull = listOfNotNull(
        executeTask,
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
    fun from(msg: TaskExecutorMessage) = when (msg) {
      is ExecuteTask -> TaskExecutorEnvelope(
          serviceName = msg.serviceName,
          type = TaskExecutorMessageType.EXECUTE_TASK,
          executeTask = msg,
      )

      is TaskStarted -> TaskExecutorEnvelope(
          serviceName = msg.serviceName,
          type = TaskExecutorMessageType.TASK_STARTED,
          taskStarted = msg,
      )

      is TaskRetried -> TaskExecutorEnvelope(
          serviceName = msg.serviceName,
          type = TaskExecutorMessageType.TASK_RETRIED,
          taskRetried = msg,
      )

      is TaskFailed -> TaskExecutorEnvelope(
          serviceName = msg.serviceName,
          type = TaskExecutorMessageType.TASK_FAILED,
          taskFailed = msg,
      )

      is TaskCompleted -> TaskExecutorEnvelope(
          serviceName = msg.serviceName,
          type = TaskExecutorMessageType.TASK_COMPLETED,
          taskCompleted = msg,
      )
    }

    /** Deserialize from a byte array and an avro schema */
    fun fromByteArray(bytes: ByteArray, readerSchema: Schema) =
        AvroSerDe.readBinary(bytes, readerSchema, serializer())

    /** Current avro Schema */
    val writerSchema = AvroSerDe.currentSchema(serializer())
  }

  override fun message(): TaskExecutorMessage =
      when (type) {
        TaskExecutorMessageType.EXECUTE_TASK -> executeTask
        TaskExecutorMessageType.TASK_STARTED -> taskStarted
        TaskExecutorMessageType.TASK_RETRIED -> taskRetried
        TaskExecutorMessageType.TASK_FAILED -> taskFailed
        TaskExecutorMessageType.TASK_COMPLETED -> taskCompleted
      }!!

  fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
