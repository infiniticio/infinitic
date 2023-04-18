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

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroDefault
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.messages.Envelope
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.workflows.data.workflows.WorkflowId
import kotlinx.serialization.Serializable
import org.apache.avro.Schema

@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class WorkflowEngineEnvelope(
    private val workflowId: WorkflowId,
    @AvroNamespace("io.infinitic.workflows.engine") private val type: WorkflowEngineMessageType,
    private val dispatchWorkflow: DispatchWorkflow? = null,
    private val dispatchMethod: DispatchMethod? = null,
    private val waitWorkflow: WaitWorkflow? = null,
    private val cancelWorkflow: CancelWorkflow? = null,
    private val retryWorkflowTask: RetryWorkflowTask? = null,
    private val retryTasks: RetryTasks? = null,
    @AvroDefault(Avro.NULL) private val completeTimers: CompleteTimers? = null,
    private val completeWorkflow: CompleteWorkflow? = null,
    private val sendSignal: SendSignal? = null,
    private val timerCompleted: TimerCompleted? = null,
    private val childMethodUnknown: ChildMethodUnknown? = null,
    private val childMethodCanceled: ChildMethodCanceled? = null,
    private val childMethodFailed: ChildMethodFailed? = null,
    private val childMethodCompleted: ChildMethodCompleted? = null,
    private val taskCanceled: TaskCanceled? = null,
    private val taskFailed: TaskFailed? = null,
    private val taskCompleted: TaskCompleted? = null
) : Envelope<WorkflowEngineMessage> {
  init {
    val noNull =
        listOfNotNull(
            dispatchWorkflow,
            dispatchMethod,
            waitWorkflow,
            cancelWorkflow,
            retryWorkflowTask,
            retryTasks,
            completeTimers,
            completeWorkflow,
            sendSignal,
            timerCompleted,
            childMethodUnknown,
            childMethodFailed,
            childMethodCanceled,
            childMethodCompleted,
            taskCanceled,
            taskFailed,
            taskCompleted)

    require(noNull.size == 1) {
      if (noNull.size > 1) {
        "More than 1 message provided: ${noNull.joinToString()}"
      } else {
        "No message provided"
      }
    }

    require(noNull.first() == message()) {
      "Provided type $type inconsistent with message ${noNull.first()}"
    }

    require(noNull.first().workflowId == workflowId) {
      "Provided workflowId $workflowId inconsistent with message ${noNull.first()}"
    }
  }

  companion object {
    fun from(msg: WorkflowEngineMessage) =
        when (msg) {
          is DispatchWorkflow ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.DISPATCH_WORKFLOW,
                  dispatchWorkflow = msg)
          is DispatchMethod ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.DISPATCH_METHOD,
                  dispatchMethod = msg)
          is WaitWorkflow ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.WAIT_WORKFLOW,
                  waitWorkflow = msg)
          is CancelWorkflow ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.CANCEL_WORKFLOW,
                  cancelWorkflow = msg)
          is RetryWorkflowTask ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.RETRY_WORKFLOW_TASK,
                  retryWorkflowTask = msg)
          is RetryTasks ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.RETRY_TASKS,
                  retryTasks = msg)
          is CompleteTimers ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.COMPLETE_TIMERS,
                  completeTimers = msg)
          is CompleteWorkflow ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.COMPLETE_WORKFLOW,
                  completeWorkflow = msg)
          is SendSignal ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.SEND_SIGNAL,
                  sendSignal = msg)
          is TimerCompleted ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.TIMER_COMPLETED,
                  timerCompleted = msg)
          is ChildMethodUnknown ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.CHILD_WORKFLOW_UNKNOWN,
                  childMethodUnknown = msg)
          is ChildMethodCanceled ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.CHILD_WORKFLOW_CANCELED,
                  childMethodCanceled = msg)
          is ChildMethodFailed ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.CHILD_WORKFLOW_FAILED,
                  childMethodFailed = msg)
          is ChildMethodCompleted ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.CHILD_WORKFLOW_COMPLETED,
                  childMethodCompleted = msg)
          is TaskCanceled ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.TASK_CANCELED,
                  taskCanceled = msg)
          is TaskFailed ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.TASK_FAILED,
                  taskFailed = msg)
          is TaskCompleted ->
              WorkflowEngineEnvelope(
                  workflowId = msg.workflowId,
                  type = WorkflowEngineMessageType.TASK_COMPLETED,
                  taskCompleted = msg)
        }

    /** Deserialize from a byte array and an avro schema */
    fun fromByteArray(bytes: ByteArray, readerSchema: Schema) =
        AvroSerDe.readBinary(bytes, serializer(), readerSchema)

    /** Current avro Schema */
    val writerSchema = AvroSerDe.schema(serializer())
  }

  override fun message(): WorkflowEngineMessage =
      when (type) {
        WorkflowEngineMessageType.DISPATCH_WORKFLOW -> dispatchWorkflow!!
        WorkflowEngineMessageType.DISPATCH_METHOD -> dispatchMethod!!
        WorkflowEngineMessageType.WAIT_WORKFLOW -> waitWorkflow!!
        WorkflowEngineMessageType.CANCEL_WORKFLOW -> cancelWorkflow!!
        WorkflowEngineMessageType.RETRY_WORKFLOW_TASK -> retryWorkflowTask!!
        WorkflowEngineMessageType.RETRY_TASKS -> retryTasks!!
        WorkflowEngineMessageType.COMPLETE_TIMERS -> completeTimers!!
        WorkflowEngineMessageType.COMPLETE_WORKFLOW -> completeWorkflow!!
        WorkflowEngineMessageType.SEND_SIGNAL -> sendSignal!!
        WorkflowEngineMessageType.TIMER_COMPLETED -> timerCompleted!!
        WorkflowEngineMessageType.CHILD_WORKFLOW_UNKNOWN -> childMethodUnknown!!
        WorkflowEngineMessageType.CHILD_WORKFLOW_CANCELED -> childMethodCanceled!!
        WorkflowEngineMessageType.CHILD_WORKFLOW_FAILED -> childMethodFailed!!
        WorkflowEngineMessageType.CHILD_WORKFLOW_COMPLETED -> childMethodCompleted!!
        WorkflowEngineMessageType.TASK_CANCELED -> taskCanceled!!
        WorkflowEngineMessageType.TASK_FAILED -> taskFailed!!
        WorkflowEngineMessageType.TASK_COMPLETED -> taskCompleted!!
      }

  fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
