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

package io.infinitic.events.types

private const val TYPE_DOMAIN = "infinitic"

private const val TYPE_TASK = "$TYPE_DOMAIN.task"

private const val TYPE_WORKFLOW = "$TYPE_DOMAIN.workflow"
private const val TYPE_WORKFLOW_EXECUTOR = "$TYPE_WORKFLOW.executor"
private const val TYPE_WORKFLOW_METHOD = "$TYPE_WORKFLOW.method"
private const val TYPE_WORKFLOW_SIGNAL = "$TYPE_WORKFLOW.signal"
private const val TYPE_REMOTE_TASK = "$TYPE_WORKFLOW.remote.task"
private const val TYPE_REMOTE_TIMER = "$TYPE_WORKFLOW.remote.timer"
private const val TYPE_REMOTE_WORKFLOW = "$TYPE_WORKFLOW.remote.workflow"

// events at task level
const val TASK_COMMANDED = "$TYPE_TASK.commanded"
const val TASK_STARTED = "$TYPE_TASK.started"
const val TASK_COMPLETED = "$TYPE_TASK.completed"
const val TASK_FAILED = "$TYPE_TASK.failed"
const val TASK_RETRY_SCHEDULED = "$TYPE_TASK.retryScheduled"

// events at workflow level
const val WORKFLOW_COMMANDED = "$TYPE_WORKFLOW.commanded"
const val WORKFLOW_COMPLETED = "$TYPE_WORKFLOW.completed"
const val WORKFLOW_CANCEL_COMMANDED = "$TYPE_WORKFLOW.cancelCommanded"
const val WORKFLOW_CANCELED = "$TYPE_WORKFLOW.canceled"

const val WORKFLOW_TASKS_RETRY_COMMANDED = "$TYPE_REMOTE_TASK.retryCommanded"
const val WORKFLOW_TASKS_RETRIED = "$TYPE_REMOTE_TASK.retried"

const val SIGNAL_COMMANDED = "$TYPE_WORKFLOW_SIGNAL.commanded"
const val SIGNAL_RECEIVED = "$TYPE_WORKFLOW_SIGNAL.received"
const val SIGNAL_DISCARDED = "$TYPE_WORKFLOW_SIGNAL.discarded"

// events related to workflow executor
const val WORKFLOW_EXECUTOR_DISPATCHED = "$TYPE_WORKFLOW_EXECUTOR.dispatched"
const val WORKFLOW_EXECUTOR_COMPLETED = "$TYPE_WORKFLOW_EXECUTOR.completed"
const val WORKFLOW_EXECUTOR_FAILED = "$TYPE_WORKFLOW_EXECUTOR.failed"
const val WORKFLOW_EXECUTOR_RETRY_COMMANDED = "$TYPE_WORKFLOW_EXECUTOR.retryCommanded"
const val WORKFLOW_EXECUTOR_RETRIED = "$TYPE_WORKFLOW_EXECUTOR.retried"

// events related to workflow methods
const val METHOD_CANCEL_COMMANDED = "$TYPE_WORKFLOW_METHOD.cancelCommanded"
const val METHOD_CANCELED = "$TYPE_WORKFLOW_METHOD.canceled"
const val METHOD_COMMANDED = "$TYPE_WORKFLOW_METHOD.commanded"
const val METHOD_COMPLETED = "$TYPE_WORKFLOW_METHOD.completed"
const val METHOD_FAILED = "$TYPE_WORKFLOW_METHOD.failed"
const val METHOD_TIMED_OUT = "$TYPE_WORKFLOW_METHOD.timedOut"

// events related to remote tasks
const val REMOTE_TASK_DISPATCHED = "$TYPE_REMOTE_TASK.dispatched"
const val REMOTE_TASK_COMPLETED = "$TYPE_REMOTE_TASK.completed"
const val REMOTE_TASK_FAILED = "$TYPE_REMOTE_TASK.failed"
const val REMOTE_TASK_CANCELED = "$TYPE_REMOTE_TASK.canceled"
const val REMOTE_TASK_TIMED_OUT = "$TYPE_REMOTE_TASK.timedOut"

// events related to  remote timers
const val REMOTE_TIMER_DISPATCHED = "$TYPE_REMOTE_TIMER.dispatched"
const val REMOTE_TIMER_COMPLETED = "$TYPE_REMOTE_TIMER.completed"

// events related to remote workflows
const val REMOTE_WORKFLOW_DISPATCHED = "$TYPE_REMOTE_WORKFLOW.dispatched"
const val REMOTE_WORKFLOW_COMPLETED = "$TYPE_REMOTE_WORKFLOW.completed"
const val REMOTE_WORKFLOW_FAILED = "$TYPE_REMOTE_WORKFLOW.failed"
const val REMOTE_WORKFLOW_CANCELED = "$TYPE_REMOTE_WORKFLOW.canceled"
const val REMOTE_WORKFLOW_TIMED_OUT = "$TYPE_REMOTE_WORKFLOW.timedOut"
const val REMOTE_WORKFLOW_UNKNOWN = "$TYPE_REMOTE_WORKFLOW.unknown"
