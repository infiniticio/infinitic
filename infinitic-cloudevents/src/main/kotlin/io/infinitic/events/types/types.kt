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
const val TYPE_TASK = "$TYPE_DOMAIN.task"
const val TYPE_WORKFLOW = "$TYPE_DOMAIN.workflow"

const val START = "start"
const val CANCEL = "cancel"
const val ENDED = "ended"

const val STARTED = "started"
const val FAILED = "failed"
const val CANCELED = "canceled"
const val COMPLETED = "completed"
const val RETRY_SCHEDULED = "retryScheduled"
const val DELEGATION_COMPLETED = "delegationCompleted"


// events related to signals
const val SIGNAL = "signal"
const val SIGNAL_RECEIVED = "signalReceived"
const val SIGNAL_DISCARDED = "signalDiscarded"
const val SIGNAL_DISPATCHED = "signalDispatched"

// events related to workflow executor
const val RETRY_EXECUTOR = "retryExecutor"
const val EXECUTOR_DISPATCHED = "executorDispatched"
const val EXECUTOR_COMPLETED = "executorCompleted"
const val EXECUTOR_FAILED = "executorFailed"

// events related to workflow methods
const val START_METHOD = "startMethod"
const val CANCEL_METHOD = "cancelMethod"
const val METHOD_CANCELED = "methodCanceled"
const val METHOD_COMPLETED = "methodCompleted"
const val METHOD_FAILED = "methodFailed"
const val METHOD_TIMED_OUT = "methodTimedOut"

// events related to  remote timers
const val TIMER_DISPATCHED = "timerDispatched"
const val TIMER_COMPLETED = "timerCompleted"

// events related to tasks
const val RETRY_TASK = "retryTask"
const val TASK_DISPATCHED = "taskDispatched"
const val TASK_COMPLETED = "taskCompleted"
const val TASK_FAILED = "taskFailed"
const val TASK_CANCELED = "taskCanceled"
const val TASK_TIMED_OUT = "taskTimedOut"
const val TASK_UNKNOWN = "taskUnknown"

// events related to remote workflow methods
const val REMOTE_METHOD_DISPATCHED = "remoteMethodDispatched"
const val REMOTE_METHOD_COMPLETED = "remoteMethodCompleted"
const val REMOTE_METHOD_FAILED = "remoteMethodFailed"
const val REMOTE_METHOD_CANCELED = "remoteMethodCanceled"
const val REMOTE_METHOD_TIMED_OUT = "remoteMethodTimedOut"
const val REMOTE_METHOD_UNKNOWN = "remoteMethodUnknown"
