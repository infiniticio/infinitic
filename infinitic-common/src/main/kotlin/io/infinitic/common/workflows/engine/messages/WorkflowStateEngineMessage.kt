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
import io.infinitic.common.data.MillisInstant
import kotlinx.serialization.Serializable

@Serializable
sealed interface WorkflowStateEngineMessage : WorkflowMessageInterface {
  // emittedAt has been introduced in 0.13.0
  // This property represents the time this event is supposed to have been emitted:
  // * if published by a client: the publishing time (got from transport)
  // * the published time (from transport) of the event that triggered the workflow task that triggered this event
  var emittedAt: MillisInstant?
}

fun WorkflowStateEngineMessage.type(): WorkflowEngineMessageType = when (this) {
  is RemoteMethodCanceled -> WorkflowEngineMessageType.CHILD_WORKFLOW_CANCELED
  is RemoteMethodCompleted -> WorkflowEngineMessageType.CHILD_WORKFLOW_COMPLETED
  is RemoteMethodFailed -> WorkflowEngineMessageType.CHILD_WORKFLOW_FAILED
  is RemoteMethodTimedOut -> WorkflowEngineMessageType.CHILD_WORKFLOW_TIMED_OUT
  is RemoteMethodUnknown -> WorkflowEngineMessageType.CHILD_WORKFLOW_UNKNOWN
  is RemoteTaskCanceled -> WorkflowEngineMessageType.TASK_CANCELED
  is RemoteTaskCompleted -> WorkflowEngineMessageType.TASK_COMPLETED
  is RemoteTaskFailed -> WorkflowEngineMessageType.TASK_FAILED
  is RemoteTaskTimedOut -> WorkflowEngineMessageType.TASK_TIMED_OUT
  is RemoteTimerCompleted -> WorkflowEngineMessageType.TIMER_COMPLETED
  is CancelWorkflow -> WorkflowEngineMessageType.CANCEL_WORKFLOW
  is CompleteTimers -> WorkflowEngineMessageType.COMPLETE_TIMERS
  is CompleteWorkflow -> WorkflowEngineMessageType.COMPLETE_WORKFLOW
  is DispatchMethod -> WorkflowEngineMessageType.DISPATCH_METHOD
  is DispatchWorkflow -> WorkflowEngineMessageType.DISPATCH_WORKFLOW
  is RetryTasks -> WorkflowEngineMessageType.RETRY_TASKS
  is RetryWorkflowTask -> WorkflowEngineMessageType.RETRY_WORKFLOW_TASK
  is SendSignal -> WorkflowEngineMessageType.SEND_SIGNAL
  is WaitWorkflow -> WorkflowEngineMessageType.WAIT_WORKFLOW
}

@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
enum class WorkflowEngineMessageType {
  WAIT_WORKFLOW,
  CANCEL_WORKFLOW,
  RETRY_WORKFLOW_TASK,
  RETRY_TASKS,
  COMPLETE_TIMERS,
  COMPLETE_WORKFLOW,
  SEND_SIGNAL,
  DISPATCH_WORKFLOW,
  DISPATCH_METHOD,
  TIMER_COMPLETED,
  CHILD_WORKFLOW_UNKNOWN,
  CHILD_WORKFLOW_CANCELED,
  CHILD_WORKFLOW_FAILED,
  CHILD_WORKFLOW_TIMED_OUT,
  CHILD_WORKFLOW_COMPLETED,
  TASK_CANCELED,
  TASK_TIMED_OUT,
  TASK_FAILED,
  TASK_COMPLETED
}


