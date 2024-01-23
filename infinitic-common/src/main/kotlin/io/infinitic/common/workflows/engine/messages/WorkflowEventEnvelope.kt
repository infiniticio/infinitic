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
package io.infinitic.common.workflows.engine.messages

import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.messages.Envelope
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.workflows.data.workflows.WorkflowId
import kotlinx.serialization.Serializable
import org.apache.avro.Schema

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowEventEnvelope(
  private val workflowId: WorkflowId,
  private val type: WorkflowEventMessageType,
  private val workflowStartedEvent: WorkflowStartedEvent? = null,
  private val workflowCompletedEvent: WorkflowCompletedEvent? = null,
  private val workflowCanceledEvent: WorkflowCanceledEvent? = null,
  private val workflowMethodStartedEvent: WorkflowMethodStartedEvent? = null,
  private val workflowMethodCompletedEvent: WorkflowMethodCompletedEvent? = null,
  private val workflowMethodFailedEvent: WorkflowMethodFailedEvent? = null,
  private val workflowMethodCanceledEvent: WorkflowMethodCanceledEvent? = null,
  private val workflowMethodTimedOutEvent: WorkflowMethodTimedOutEvent? = null
) : Envelope<WorkflowEventMessage> {
  init {
    val noNull = listOfNotNull(
        workflowStartedEvent,
        workflowCompletedEvent,
        workflowCanceledEvent,
        workflowMethodStartedEvent,
        workflowMethodCompletedEvent,
        workflowMethodFailedEvent,
        workflowMethodCanceledEvent,
        workflowMethodTimedOutEvent,
    )

    require(noNull.size == 1)
    require(noNull.first() == message())
    require(noNull.first().workflowId == workflowId)
  }

  companion object {
    fun from(msg: WorkflowEventMessage) = when (msg) {

      is WorkflowStartedEvent -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.WORKFLOW_STARTED,
          workflowStartedEvent = msg,
      )

      is WorkflowCompletedEvent -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.WORKFLOW_COMPLETED,
          workflowCompletedEvent = msg,
      )

      is WorkflowCanceledEvent -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.WORKFLOW_CANCELED,
          workflowCanceledEvent = msg,
      )

      is WorkflowMethodStartedEvent -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.WORKFLOW_METHOD_STARTED,
          workflowMethodStartedEvent = msg,
      )

      is WorkflowMethodCompletedEvent -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.WORKFLOW_METHOD_COMPLETED,
          workflowMethodCompletedEvent = msg,
      )

      is WorkflowMethodFailedEvent -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.WORKFLOW_METHOD_FAILED,
          workflowMethodFailedEvent = msg,
      )

      is WorkflowMethodCanceledEvent -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.WORKFLOW_METHOD_CANCELED,
          workflowMethodCanceledEvent = msg,
      )

      is WorkflowMethodTimedOutEvent -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.WORKFLOW_METHOD_TIMED_OUT,
          workflowMethodTimedOutEvent = msg,
      )
    }

    /** Deserialize from a byte array and an avro schema */
    fun fromByteArray(bytes: ByteArray, readerSchema: Schema): WorkflowEventEnvelope =
        AvroSerDe.readBinary(bytes, readerSchema, serializer())

    /** Current avro Schema */
    val writerSchema = AvroSerDe.currentSchema(serializer())
  }

  override fun message(): WorkflowEventMessage = when (type) {
    WorkflowEventMessageType.WORKFLOW_STARTED -> workflowStartedEvent
    WorkflowEventMessageType.WORKFLOW_COMPLETED -> workflowCompletedEvent
    WorkflowEventMessageType.WORKFLOW_CANCELED -> workflowCanceledEvent
    WorkflowEventMessageType.WORKFLOW_METHOD_STARTED -> workflowMethodStartedEvent
    WorkflowEventMessageType.WORKFLOW_METHOD_COMPLETED -> workflowMethodCompletedEvent
    WorkflowEventMessageType.WORKFLOW_METHOD_FAILED -> workflowMethodFailedEvent
    WorkflowEventMessageType.WORKFLOW_METHOD_CANCELED -> workflowMethodCanceledEvent
    WorkflowEventMessageType.WORKFLOW_METHOD_TIMED_OUT -> workflowMethodTimedOutEvent
  }!!

  fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
