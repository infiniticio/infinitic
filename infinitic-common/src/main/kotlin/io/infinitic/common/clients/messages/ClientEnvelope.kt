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
package io.infinitic.common.clients.messages

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroDefault
import com.github.avrokotlin.avro4k.AvroName
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.messages.Envelope
import io.infinitic.common.serDe.avro.AvroSerDe
import kotlinx.serialization.Serializable
import org.apache.avro.Schema

@Serializable
data class ClientEnvelope(
  private val clientName: ClientName,
  private val type: ClientMessageType,
  private val taskCompleted: TaskCompleted? = null,
  private val taskCanceled: TaskCanceled? = null,
  private val taskFailed: TaskFailed? = null,
  private val taskIdsByTag: TaskIdsByTag? = null,
  private val workflowCompleted: MethodCompleted? = null,
  private val workflowCanceled: MethodCanceled? = null,
  private val workflowFailed: MethodFailed? = null,
  @AvroDefault(Avro.NULL) private val workflowTimedOut: MethodTimedOut? = null,
  @AvroName("unknownWorkflow") private val workflowUnknown: MethodUnknown? = null,
  private val methodAlreadyCompleted: MethodAlreadyCompleted? = null,
  private val workflowIdsByTag: WorkflowIdsByTag? = null
) : Envelope<ClientMessage> {
  init {
    val noNull = listOfNotNull(
        taskCompleted,
        taskCanceled,
        taskFailed,
        taskIdsByTag,
        workflowCompleted,
        workflowCanceled,
        workflowFailed,
        workflowTimedOut,
        workflowUnknown,
        methodAlreadyCompleted,
        workflowIdsByTag,
    )

    require(noNull.size == 1)
    require(noNull.first() == message())
    require(noNull.first().recipientName == clientName)
  }

  companion object {
    fun from(msg: ClientMessage) = when (msg) {
      is TaskCompleted -> ClientEnvelope(
          clientName = msg.recipientName,
          type = ClientMessageType.TASK_COMPLETED,
          taskCompleted = msg,
      )

      is TaskCanceled -> ClientEnvelope(
          clientName = msg.recipientName,
          type = ClientMessageType.TASK_CANCELED,
          taskCanceled = msg,
      )

      is TaskFailed -> ClientEnvelope(
          clientName = msg.recipientName,
          type = ClientMessageType.TASK_FAILED,
          taskFailed = msg,
      )

      is TaskIdsByTag -> ClientEnvelope(
          clientName = msg.recipientName,
          type = ClientMessageType.TASK_IDS_PER_TAG,
          taskIdsByTag = msg,
      )

      is MethodCompleted -> ClientEnvelope(
          clientName = msg.recipientName,
          type = ClientMessageType.WORKFLOW_COMPLETED,
          workflowCompleted = msg,
      )

      is MethodCanceled -> ClientEnvelope(
          clientName = msg.recipientName,
          type = ClientMessageType.WORKFLOW_CANCELED,
          workflowCanceled = msg,
      )

      is MethodFailed -> ClientEnvelope(
          clientName = msg.recipientName,
          type = ClientMessageType.WORKFLOW_FAILED,
          workflowFailed = msg,
      )

      is MethodTimedOut -> ClientEnvelope(
          clientName = msg.recipientName,
          type = ClientMessageType.WORKFLOW_TIMED_OUT,
          workflowTimedOut = msg,
      )

      is MethodUnknown -> ClientEnvelope(
          clientName = msg.recipientName,
          type = ClientMessageType.UNKNOWN_WORKFLOW,
          workflowUnknown = msg,
      )

      is MethodAlreadyCompleted -> ClientEnvelope(
          clientName = msg.recipientName,
          type = ClientMessageType.WORKFLOW_ALREADY_COMPLETED,
          methodAlreadyCompleted = msg,
      )

      is WorkflowIdsByTag -> ClientEnvelope(
          clientName = msg.recipientName,
          type = ClientMessageType.WORKFLOW_IDS_PER_TAG,
          workflowIdsByTag = msg,
      )
    }

    /** Deserialize from a byte array and an avro schema */
    fun fromByteArray(bytes: ByteArray, readerSchema: Schema) =
        AvroSerDe.readBinary(bytes, readerSchema, serializer())

    /** Current avro Schema */
    val writerSchema = AvroSerDe.currentSchema(serializer())
  }

  override fun message(): ClientMessage =
      when (type) {
        ClientMessageType.TASK_COMPLETED -> taskCompleted
        ClientMessageType.TASK_CANCELED -> taskCanceled
        ClientMessageType.TASK_FAILED -> taskFailed
        ClientMessageType.TASK_IDS_PER_TAG -> taskIdsByTag
        ClientMessageType.WORKFLOW_COMPLETED -> workflowCompleted
        ClientMessageType.WORKFLOW_CANCELED -> workflowCanceled
        ClientMessageType.WORKFLOW_FAILED -> workflowFailed
        ClientMessageType.WORKFLOW_TIMED_OUT -> workflowTimedOut
        ClientMessageType.UNKNOWN_WORKFLOW -> workflowUnknown
        ClientMessageType.WORKFLOW_ALREADY_COMPLETED -> methodAlreadyCompleted
        ClientMessageType.WORKFLOW_IDS_PER_TAG -> workflowIdsByTag
      }!!

  fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
