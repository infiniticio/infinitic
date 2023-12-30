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
package io.infinitic.common.workflows.engine.events

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
  private val workflowStarted: WorkflowStarted? = null,
  private val workflowCompleted: WorkflowCompleted? = null,
  private val workflowCanceled: WorkflowCanceled? = null,
  private val workflowMethodStarted: WorkflowMethodStarted? = null,
  private val workflowMethodCompleted: WorkflowMethodCompleted? = null,
  private val workflowMethodFailed: WorkflowMethodFailed? = null,
  private val workflowMethodCanceled: WorkflowMethodCanceled? = null,
  private val workflowMethodTimedOut: WorkflowMethodTimedOut? = null
) : Envelope<WorkflowEventMessage> {
  init {
    val noNull = listOfNotNull(
        workflowStarted,
        workflowCompleted,
        workflowCanceled,
        workflowMethodStarted,
        workflowMethodCompleted,
        workflowMethodFailed,
        workflowMethodCanceled,
        workflowMethodTimedOut,
    )

    require(noNull.size == 1)
    require(noNull.first() == message())
    require(noNull.first().workflowId == workflowId)
  }

  companion object {
    fun from(msg: WorkflowEventMessage) = when (msg) {

      is WorkflowStarted -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.WORKFLOW_STARTED,
          workflowStarted = msg,
      )

      is WorkflowCompleted -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.WORKFLOW_COMPLETED,
          workflowCompleted = msg,
      )

      is WorkflowCanceled -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.WORKFLOW_CANCELED,
          workflowCanceled = msg,
      )

      is WorkflowMethodStarted -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.WORKFLOW_METHOD_STARTED,
          workflowMethodStarted = msg,
      )

      is WorkflowMethodCompleted -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.WORKFLOW_METHOD_COMPLETED,
          workflowMethodCompleted = msg,
      )

      is WorkflowMethodFailed -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.WORKFLOW_METHOD_FAILED,
          workflowMethodFailed = msg,
      )

      is WorkflowMethodCanceled -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.WORKFLOW_METHOD_CANCELED,
          workflowMethodCanceled = msg,
      )

      is WorkflowMethodTimedOut -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.WORKFLOW_METHOD_TIMED_OUT,
          workflowMethodTimedOut = msg,
      )
    }

    /** Deserialize from a byte array and an avro schema */
    fun fromByteArray(bytes: ByteArray, readerSchema: Schema): WorkflowEventEnvelope =
        AvroSerDe.readBinary(bytes, readerSchema, serializer())

    /** Current avro Schema */
    val writerSchema = AvroSerDe.currentSchema(serializer())
  }

  override fun message(): WorkflowEventMessage = when (type) {
    WorkflowEventMessageType.WORKFLOW_STARTED -> workflowStarted
    WorkflowEventMessageType.WORKFLOW_COMPLETED -> workflowCompleted
    WorkflowEventMessageType.WORKFLOW_CANCELED -> workflowCanceled
    WorkflowEventMessageType.WORKFLOW_METHOD_STARTED -> workflowMethodStarted
    WorkflowEventMessageType.WORKFLOW_METHOD_COMPLETED -> workflowMethodCompleted
    WorkflowEventMessageType.WORKFLOW_METHOD_FAILED -> workflowMethodFailed
    WorkflowEventMessageType.WORKFLOW_METHOD_CANCELED -> workflowMethodCanceled
    WorkflowEventMessageType.WORKFLOW_METHOD_TIMED_OUT -> workflowMethodTimedOut
  }!!

  fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
