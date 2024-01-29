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
  private val workflowCompletedEvent: WorkflowCompletedEvent? = null,
  private val workflowCanceledEvent: WorkflowCanceledEvent? = null,
  private val methodDispatchedEvent: MethodDispatchedEvent? = null,
  private val methodCompletedEvent: MethodCompletedEvent? = null,
  private val methodFailedEvent: MethodFailedEvent? = null,
  private val methodCanceledEvent: MethodCanceledEvent? = null,
  private val methodTimedOutEvent: MethodTimedOutEvent? = null,
  private val taskDispatched: TaskDispatchedEvent? = null,
  private val childDispatched: ChildMethodDispatchedEvent? = null,
) : Envelope<WorkflowEventMessage> {
  init {
    val noNull = listOfNotNull(
        workflowCompletedEvent,
        workflowCanceledEvent,
        methodDispatchedEvent,
        methodCompletedEvent,
        methodFailedEvent,
        methodCanceledEvent,
        methodTimedOutEvent,
        taskDispatched,
        childDispatched,
    )

    require(noNull.size == 1)
    require(noNull.first() == message())
    require(noNull.first().workflowId == workflowId)
  }

  companion object {
    fun from(msg: WorkflowEventMessage) = when (msg) {

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

      is MethodDispatchedEvent -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.METHOD_DISPATCHED,
          methodDispatchedEvent = msg,
      )

      is MethodCompletedEvent -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.METHOD_COMPLETED,
          methodCompletedEvent = msg,
      )

      is MethodFailedEvent -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.METHOD_FAILED,
          methodFailedEvent = msg,
      )

      is MethodCanceledEvent -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.METHOD_CANCELED,
          methodCanceledEvent = msg,
      )

      is MethodTimedOutEvent -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.METHOD_TIMED_OUT,
          methodTimedOutEvent = msg,
      )

      is TaskDispatchedEvent -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.METHOD_TASK_DISPATCHED,
          taskDispatched = msg,
      )

      is ChildMethodDispatchedEvent -> WorkflowEventEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowEventMessageType.METHOD_CHILD_DISPATCHED,
          childDispatched = msg,
      )
    }

    /** Deserialize from a byte array and an avro schema */
    fun fromByteArray(bytes: ByteArray, readerSchema: Schema): WorkflowEventEnvelope =
        AvroSerDe.readBinary(bytes, readerSchema, serializer())

    /** Current avro Schema */
    val writerSchema = AvroSerDe.currentSchema(serializer())
  }

  override fun message(): WorkflowEventMessage = when (type) {
    WorkflowEventMessageType.WORKFLOW_COMPLETED -> workflowCompletedEvent
    WorkflowEventMessageType.WORKFLOW_CANCELED -> workflowCanceledEvent
    WorkflowEventMessageType.METHOD_DISPATCHED -> methodDispatchedEvent
    WorkflowEventMessageType.METHOD_COMPLETED -> methodCompletedEvent
    WorkflowEventMessageType.METHOD_FAILED -> methodFailedEvent
    WorkflowEventMessageType.METHOD_CANCELED -> methodCanceledEvent
    WorkflowEventMessageType.METHOD_TIMED_OUT -> methodTimedOutEvent
    WorkflowEventMessageType.METHOD_TASK_DISPATCHED -> taskDispatched
    WorkflowEventMessageType.METHOD_CHILD_DISPATCHED -> childDispatched
  }!!

  fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
