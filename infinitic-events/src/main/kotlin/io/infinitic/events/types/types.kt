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
private const val TYPE_WORKFLOW_TASK = "$TYPE_WORKFLOW_METHOD.task"
private const val TYPE_WORKFLOW_TIMER = "$TYPE_WORKFLOW_METHOD.timer"
private const val TYPE_WORKFLOW_CHILD = "$TYPE_WORKFLOW_METHOD.childWorkflow"

// events at task level
const val TASK_COMMANDED = "$TYPE_TASK.commanded"
const val TASK_STARTED = "$TYPE_TASK.started"
const val TASK_COMPLETED = "$TYPE_TASK.completed"
const val TASK_FAILED = "$TYPE_TASK.failed"
const val TASK_RETRIED = "$TYPE_TASK.failedRetried"

// events at workflow level
const val WORKFLOW_COMMANDED = "$TYPE_WORKFLOW.commanded"
const val WORKFLOW_COMPLETED = "$TYPE_WORKFLOW.completed"
const val WORKFLOW_CANCELED = "$TYPE_WORKFLOW.canceled"

const val WORKFLOW_TASKS_RETRY_COMMANDED = "$TYPE_WORKFLOW.tasksRetryCommanded"
const val WORKFLOW_TASKS_RETRIED = "$TYPE_WORKFLOW.tasksRetried"

const val WORKFLOW_SIGNAL_COMMANDED = "$TYPE_WORKFLOW_SIGNAL.commanded"
const val WORKFLOW_SIGNAL_RECEIVED = "$TYPE_WORKFLOW_SIGNAL.received"
const val WORKFLOW_SIGNAL_DISCARDED = "$TYPE_WORKFLOW_SIGNAL.discarded"

// events related to workflow task executor
const val WORKFLOW_EXECUTOR_DISPATCHED = "$TYPE_WORKFLOW_EXECUTOR.dispatched"
const val WORKFLOW_EXECUTOR_COMPLETED = "$TYPE_WORKFLOW_EXECUTOR.completed"
const val WORKFLOW_EXECUTOR_FAILED = "$TYPE_WORKFLOW_EXECUTOR.failed"

const val WORKFLOW_EXECUTOR_RETRY_COMMANDED = "$TYPE_WORKFLOW_EXECUTOR.retryCommanded"
const val WORKFLOW_EXECUTOR_RETRIED = "$TYPE_WORKFLOW_EXECUTOR.retried"

// events related to workflow methods
const val WORKFLOW_METHOD_COMMANDED = "$TYPE_WORKFLOW_METHOD.commanded"
const val WORKFLOW_METHOD_COMPLETED = "$TYPE_WORKFLOW_METHOD.completed"
const val WORKFLOW_METHOD_FAILED = "$TYPE_WORKFLOW_METHOD.failed"
const val WORKFLOW_METHOD_TIMED_OUT = "$TYPE_WORKFLOW_METHOD.timedOut"
const val WORKFLOW_METHOD_CANCELED = "$TYPE_WORKFLOW_METHOD.canceled"

// events related to tasks in workflow methods
const val WORKFLOW_TASK_DISPATCHED = "$TYPE_WORKFLOW_TASK.dispatched"
const val WORKFLOW_TASK_COMPLETED = "$TYPE_WORKFLOW_TASK.completed"
const val WORKFLOW_TASK_FAILED = "$TYPE_WORKFLOW_TASK.failed"
const val WORKFLOW_TASK_CANCELED = "$TYPE_WORKFLOW_TASK.canceled"
const val WORKFLOW_TASK_TIMED_OUT = "$TYPE_WORKFLOW_TASK.timedOut"

// events related to timers in workflow methods
const val WORKFLOW_TIMER_DISPATCHED = "$TYPE_WORKFLOW_TIMER.dispatched"
const val WORKFLOW_TIMER_COMPLETED = "$TYPE_WORKFLOW_TIMER.completed"

// events related to child workflows in workflow methods
const val WORKFLOW_CHILD_DISPATCHED = "$TYPE_WORKFLOW_CHILD.dispatched"
const val WORKFLOW_CHILD_COMPLETED = "$TYPE_WORKFLOW_CHILD.completed"
const val WORKFLOW_CHILD_FAILED = "$TYPE_WORKFLOW_CHILD.failed"
const val WORKFLOW_CHILD_CANCELED = "$TYPE_WORKFLOW_CHILD.canceled"
const val WORKFLOW_CHILD_TIMED_OUT = "$TYPE_WORKFLOW_CHILD.timedOut"
const val WORKFLOW_CHILD_UNKNOWN = "$TYPE_WORKFLOW_CHILD.unknown"
