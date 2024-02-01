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


private const val TYPE_DOMAIN = "io.infinitic"
const val TYPE_TASK = "$TYPE_DOMAIN.task"
const val TYPE_WORKFLOW = "$TYPE_DOMAIN.workflow"

// actions
const val COMMANDED = "start"
const val ENDED = "ended"
const val COMPLETED = "completed"
const val CANCELED = "canceled"
const val STARTED = "started"
const val FAILED = "failed"
const val RETRY_SCHEDULED = "retryScheduled"

const val CANCEL_COMMANDED = "cancel"

const val REMOTE_TASKS_RETRY_COMMANDED = "retryTasks"
const val REMOTE_TASKS_RETRIED = "tasksRetried"

// events related to signals
const val SIGNAL_COMMANDED = "signal"
const val SIGNAL_HANDLED = "signalHandled"
const val SIGNAL_DISCARDED = "signalDiscarded"

// events related to workflow executor
const val EXECUTOR_DISPATCHED = "executorDispatched"
const val EXECUTOR_COMPLETED = "executorCompleted"
const val EXECUTOR_FAILED = "executorFailed"
const val EXECUTOR_RETRY_COMMANDED = "retryExecutor"
const val EXECUTOR_RETRIED = "executorRetried"

// events related to workflow methods
const val METHOD_CANCEL_COMMANDED = "cancelMethod"
const val METHOD_CANCELED = "methodCanceled"
const val METHOD_COMMANDED = "startMethod"
const val METHOD_COMPLETED = "methodCompleted"
const val METHOD_FAILED = "methodFailed"
const val METHOD_TIMED_OUT = "methodTimedOut"

// events related to  remote timers
const val REMOTE_TIMER_DISPATCHED = "remoteTimerDispatched"
const val REMOTE_TIMER_COMPLETED = "remoteTimerCompleted"

// events related to remote tasks
const val REMOTE_TASK_DISPATCHED = "taskDispatched"
const val REMOTE_TASK_COMPLETED = "taskCompleted"
const val REMOTE_TASK_FAILED = "taskFailed"
const val REMOTE_TASK_CANCELED = "taskCanceled"
const val REMOTE_TASK_TIMED_OUT = "taskTimedOut"
const val REMOTE_TASK_UNKNOWN = "taskUnknown"

// events related to remote workflow methods
const val REMOTE_METHOD_DISPATCHED = "remoteMethodDispatched"
const val REMOTE_METHOD_COMPLETED = "remoteMethodCompleted"
const val REMOTE_METHOD_FAILED = "remoteMethodFailed"
const val REMOTE_METHOD_CANCELED = "remoteMethodCanceled"
const val REMOTE_METHOD_TIMED_OUT = "remoteMethodTimedOut"
const val REMOTE_METHOD_UNKNOWN = "remoteMethodUnknown"
