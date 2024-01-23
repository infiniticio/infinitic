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
@AvroNamespace("io.infinitic.workflows.engine")
data class WorkflowCmdEnvelope(
  private val workflowId: WorkflowId,
  private val type: WorkflowCmdMessageType,
  private val dispatchWorkflow: DispatchNewWorkflow? = null,
  private val dispatchMethod: DispatchMethodWorkflow? = null,
  private val waitWorkflow: WaitWorkflow? = null,
  private val cancelWorkflow: CancelWorkflow? = null,
  private val retryWorkflowTask: RetryWorkflowTask? = null,
  private val retryTasks: RetryTasks? = null,
  private val completeTimers: CompleteTimers? = null,
  private val completeWorkflow: CompleteWorkflow? = null,
  private val sendSignal: SendSignal? = null,
) : Envelope<WorkflowCmdMessage> {
  init {
    val noNull = listOfNotNull(
        dispatchWorkflow,
        dispatchMethod,
        waitWorkflow,
        cancelWorkflow,
        retryWorkflowTask,
        retryTasks,
        completeTimers,
        completeWorkflow,
        sendSignal,
    )

    require(noNull.size == 1)
    require(noNull.first() == message())
    require(noNull.first().workflowId == workflowId)
  }

  companion object {
    fun from(msg: WorkflowCmdMessage) = when (msg) {

      is DispatchNewWorkflow -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowCmdMessageType.DISPATCH_WORKFLOW,
          dispatchWorkflow = msg,
      )

      is DispatchMethodWorkflow -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowCmdMessageType.DISPATCH_METHOD,
          dispatchMethod = msg,
      )

      is WaitWorkflow -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowCmdMessageType.WAIT_WORKFLOW,
          waitWorkflow = msg,
      )

      is CancelWorkflow -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowCmdMessageType.CANCEL_WORKFLOW,
          cancelWorkflow = msg,
      )

      is RetryWorkflowTask -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowCmdMessageType.RETRY_WORKFLOW_TASK,
          retryWorkflowTask = msg,
      )

      is RetryTasks -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowCmdMessageType.RETRY_TASKS,
          retryTasks = msg,
      )

      is CompleteTimers -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowCmdMessageType.COMPLETE_TIMERS,
          completeTimers = msg,
      )

      is CompleteWorkflow -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowCmdMessageType.COMPLETE_WORKFLOW,
          completeWorkflow = msg,
      )

      is SendSignal -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowCmdMessageType.SEND_SIGNAL,
          sendSignal = msg,
      )
    }

    /** Deserialize from a byte array and an avro schema */
    fun fromByteArray(bytes: ByteArray, readerSchema: Schema): WorkflowCmdEnvelope =
        AvroSerDe.readBinary(bytes, readerSchema, serializer())

    /** Current avro Schema */
    val writerSchema = AvroSerDe.currentSchema(serializer())
  }

  override fun message(): WorkflowCmdMessage = when (type) {
    WorkflowCmdMessageType.DISPATCH_WORKFLOW -> dispatchWorkflow
    WorkflowCmdMessageType.DISPATCH_METHOD -> dispatchMethod
    WorkflowCmdMessageType.WAIT_WORKFLOW -> waitWorkflow
    WorkflowCmdMessageType.CANCEL_WORKFLOW -> cancelWorkflow
    WorkflowCmdMessageType.RETRY_WORKFLOW_TASK -> retryWorkflowTask
    WorkflowCmdMessageType.RETRY_TASKS -> retryTasks
    WorkflowCmdMessageType.COMPLETE_TIMERS -> completeTimers
    WorkflowCmdMessageType.COMPLETE_WORKFLOW -> completeWorkflow
    WorkflowCmdMessageType.SEND_SIGNAL -> sendSignal
  }!!

  fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
