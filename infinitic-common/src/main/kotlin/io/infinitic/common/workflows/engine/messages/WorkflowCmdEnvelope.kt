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
  private val type: WorkflowStateEngineCmdMessageType,
  private val dispatchWorkflow: DispatchWorkflow? = null,
  private val dispatchMethod: DispatchMethod? = null,
  private val waitWorkflow: WaitWorkflow? = null,
  private val cancelWorkflow: CancelWorkflow? = null,
  private val retryWorkflowTask: RetryWorkflowTask? = null,
  private val retryTasks: RetryTasks? = null,
  private val completeTimers: CompleteTimers? = null,
  private val completeWorkflow: CompleteWorkflow? = null,
  private val sendSignal: SendSignal? = null,
) : Envelope<WorkflowStateEngineCmdMessage> {
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
    fun from(msg: WorkflowStateEngineCmdMessage) = when (msg) {

      is DispatchWorkflow -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowStateEngineCmdMessageType.DISPATCH_WORKFLOW,
          dispatchWorkflow = msg,
      )

      is DispatchMethod -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowStateEngineCmdMessageType.DISPATCH_METHOD,
          dispatchMethod = msg,
      )

      is WaitWorkflow -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowStateEngineCmdMessageType.WAIT_WORKFLOW,
          waitWorkflow = msg,
      )

      is CancelWorkflow -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowStateEngineCmdMessageType.CANCEL_WORKFLOW,
          cancelWorkflow = msg,
      )

      is RetryWorkflowTask -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowStateEngineCmdMessageType.RETRY_WORKFLOW_TASK,
          retryWorkflowTask = msg,
      )

      is RetryTasks -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowStateEngineCmdMessageType.RETRY_TASKS,
          retryTasks = msg,
      )

      is CompleteTimers -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowStateEngineCmdMessageType.COMPLETE_TIMERS,
          completeTimers = msg,
      )

      is CompleteWorkflow -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowStateEngineCmdMessageType.COMPLETE_WORKFLOW,
          completeWorkflow = msg,
      )

      is SendSignal -> WorkflowCmdEnvelope(
          workflowId = msg.workflowId,
          type = WorkflowStateEngineCmdMessageType.SEND_SIGNAL,
          sendSignal = msg,
      )
    }

    /** Deserialize from a byte array and an avro schema */
    fun fromByteArray(bytes: ByteArray, readerSchema: Schema): WorkflowCmdEnvelope =
        AvroSerDe.readBinary(bytes, readerSchema, serializer())

    /** Current avro Schema */
    val writerSchema = AvroSerDe.currentSchema(serializer())
  }

  override fun message(): WorkflowStateEngineCmdMessage = when (type) {
    WorkflowStateEngineCmdMessageType.DISPATCH_WORKFLOW -> dispatchWorkflow
    WorkflowStateEngineCmdMessageType.DISPATCH_METHOD -> dispatchMethod
    WorkflowStateEngineCmdMessageType.WAIT_WORKFLOW -> waitWorkflow
    WorkflowStateEngineCmdMessageType.CANCEL_WORKFLOW -> cancelWorkflow
    WorkflowStateEngineCmdMessageType.RETRY_WORKFLOW_TASK -> retryWorkflowTask
    WorkflowStateEngineCmdMessageType.RETRY_TASKS -> retryTasks
    WorkflowStateEngineCmdMessageType.COMPLETE_TIMERS -> completeTimers
    WorkflowStateEngineCmdMessageType.COMPLETE_WORKFLOW -> completeWorkflow
    WorkflowStateEngineCmdMessageType.SEND_SIGNAL -> sendSignal
  }!!

  fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
