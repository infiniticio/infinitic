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

package io.infinitic.events.data.workflows

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.CompleteWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.MethodCanceledEvent
import io.infinitic.common.workflows.engine.messages.MethodCommandedEvent
import io.infinitic.common.workflows.engine.messages.MethodCompletedEvent
import io.infinitic.common.workflows.engine.messages.MethodFailedEvent
import io.infinitic.common.workflows.engine.messages.MethodTimedOutEvent
import io.infinitic.common.workflows.engine.messages.RemoteMethodCanceled
import io.infinitic.common.workflows.engine.messages.RemoteMethodCompleted
import io.infinitic.common.workflows.engine.messages.RemoteMethodDispatchedEvent
import io.infinitic.common.workflows.engine.messages.RemoteMethodFailed
import io.infinitic.common.workflows.engine.messages.RemoteMethodTimedOut
import io.infinitic.common.workflows.engine.messages.RemoteMethodUnknown
import io.infinitic.common.workflows.engine.messages.RemoteTaskCanceled
import io.infinitic.common.workflows.engine.messages.RemoteTaskCompleted
import io.infinitic.common.workflows.engine.messages.RemoteTaskFailed
import io.infinitic.common.workflows.engine.messages.RemoteTaskTimedOut
import io.infinitic.common.workflows.engine.messages.RemoteTimerCompleted
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.SignalDiscardedEvent
import io.infinitic.common.workflows.engine.messages.SignalDispatchedEvent
import io.infinitic.common.workflows.engine.messages.SignalReceivedEvent
import io.infinitic.common.workflows.engine.messages.TaskDispatchedEvent
import io.infinitic.common.workflows.engine.messages.TimerDispatchedEvent
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowCanceledEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowCompletedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.events.types.CANCEL
import io.infinitic.events.types.CANCELED
import io.infinitic.events.types.CANCEL_METHOD
import io.infinitic.events.types.ENDED
import io.infinitic.events.types.EXECUTOR_COMPLETED
import io.infinitic.events.types.EXECUTOR_DISPATCHED
import io.infinitic.events.types.EXECUTOR_FAILED
import io.infinitic.events.types.METHOD_CANCELED
import io.infinitic.events.types.METHOD_COMPLETED
import io.infinitic.events.types.METHOD_FAILED
import io.infinitic.events.types.METHOD_TIMED_OUT
import io.infinitic.events.types.REMOTE_METHOD_CANCELED
import io.infinitic.events.types.REMOTE_METHOD_COMPLETED
import io.infinitic.events.types.REMOTE_METHOD_DISPATCHED
import io.infinitic.events.types.REMOTE_METHOD_FAILED
import io.infinitic.events.types.REMOTE_METHOD_TIMED_OUT
import io.infinitic.events.types.REMOTE_METHOD_UNKNOWN
import io.infinitic.events.types.RETRY_EXECUTOR
import io.infinitic.events.types.RETRY_TASK
import io.infinitic.events.types.SIGNAL
import io.infinitic.events.types.SIGNAL_DISCARDED
import io.infinitic.events.types.SIGNAL_DISPATCHED
import io.infinitic.events.types.SIGNAL_RECEIVED
import io.infinitic.events.types.START
import io.infinitic.events.types.START_METHOD
import io.infinitic.events.types.TASK_COMPLETED
import io.infinitic.events.types.TASK_DISPATCHED
import io.infinitic.events.types.TASK_FAILED
import io.infinitic.events.types.TASK_TIMED_OUT
import io.infinitic.events.types.TIMER_COMPLETED
import io.infinitic.events.types.TIMER_DISPATCHED
import io.infinitic.events.types.TYPE_WORKFLOW

internal fun WorkflowCmdMessage.workflowSimpleType(): String? = when (this) {
  is DispatchWorkflow -> START
  is DispatchMethod -> START_METHOD
  is CancelWorkflow -> when (workflowMethodId) {
    null -> CANCEL
    else -> CANCEL_METHOD
  }

  is CompleteTimers -> null
  is CompleteWorkflow -> null
  is RetryTasks -> RETRY_TASK
  is RetryWorkflowTask -> RETRY_EXECUTOR
  is SendSignal -> SIGNAL
  is WaitWorkflow -> null
}

internal fun WorkflowStateEngineMessage.workflowSimpleType(): String? = when (this) {
  is WorkflowCmdMessage -> null
  is RemoteTimerCompleted -> TIMER_COMPLETED
  is RemoteMethodCompleted -> REMOTE_METHOD_COMPLETED
  is RemoteMethodCanceled -> REMOTE_METHOD_CANCELED
  is RemoteMethodFailed -> REMOTE_METHOD_FAILED
  is RemoteMethodTimedOut -> REMOTE_METHOD_TIMED_OUT
  is RemoteMethodUnknown -> REMOTE_METHOD_UNKNOWN
  is RemoteTaskCanceled -> null
  is RemoteTaskTimedOut -> when (isWorkflowTaskEvent()) {
    true -> thisShouldNotHappen() // Workflow tasks hava to timeout at workflow level
    false -> TASK_TIMED_OUT
  }

  is RemoteTaskFailed -> when (isWorkflowTaskEvent()) {
    true -> EXECUTOR_FAILED
    false -> TASK_FAILED
  }

  is RemoteTaskCompleted -> when (isWorkflowTaskEvent()) {
    true -> EXECUTOR_COMPLETED
    false -> TASK_COMPLETED
  }
}

internal fun WorkflowEventMessage.workflowSimpleType(): String = when (this) {
  is WorkflowCompletedEvent -> ENDED
  is WorkflowCanceledEvent -> CANCELED
  is MethodCommandedEvent -> START_METHOD
  is MethodCompletedEvent -> METHOD_COMPLETED
  is MethodFailedEvent -> METHOD_FAILED
  is MethodCanceledEvent -> METHOD_CANCELED
  is MethodTimedOutEvent -> METHOD_TIMED_OUT
  is RemoteMethodDispatchedEvent -> REMOTE_METHOD_DISPATCHED
  is TaskDispatchedEvent -> when (isWorkflowTaskEvent()) {
    true -> EXECUTOR_DISPATCHED
    false -> TASK_DISPATCHED
  }

  is TimerDispatchedEvent -> TIMER_DISPATCHED
  is SignalDispatchedEvent -> SIGNAL_DISPATCHED
  is SignalDiscardedEvent -> SIGNAL_DISCARDED
  is SignalReceivedEvent -> SIGNAL_RECEIVED
}


fun WorkflowCmdMessage.workflowType(): String? =
    this.workflowSimpleType()?.let { "$TYPE_WORKFLOW.$it" }

fun WorkflowStateEngineMessage.workflowType(): String? =
    this.workflowSimpleType()?.let { "$TYPE_WORKFLOW.$it" }

fun WorkflowEventMessage.workflowType(): String = "$TYPE_WORKFLOW." + workflowSimpleType()
