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

package io.infinitic.events

private const val TYPE_DOMAIN = "infinitic"

private const val TYPE_WORKFLOW = "$TYPE_DOMAIN.workflow"
private const val TYPE_WORKFLOW_EXECUTOR = "$TYPE_WORKFLOW.executor"
private const val TYPE_WORKFLOW_METHOD = "$TYPE_WORKFLOW.method"
private const val TYPE_WORKFLOW_METHOD_CHILD = "$TYPE_WORKFLOW_METHOD.child"
private const val TYPE_WORKFLOW_METHOD_TASK = "$TYPE_WORKFLOW_METHOD.task"
private const val TYPE_WORKFLOW_METHOD_TIMER = "$TYPE_WORKFLOW_METHOD.timer"

private const val TYPE_TASK = "$TYPE_DOMAIN.task"

enum class InfiniticEventType(val value: String) {
  // task level
  TASK_DISPATCHED("$TYPE_TASK.dispatched"),
  TASK_STARTED("$TYPE_TASK.started"),
  TASK_COMPLETED("$TYPE_TASK.completed"),
  TASK_FAILED("$TYPE_TASK.failed"),
  TASK_RETRIED("$TYPE_TASK.retried"),

  // workflow level
  WORKFLOW_DISPATCHED("$TYPE_WORKFLOW.dispatched"),
  WORKFLOW_STARTED("$TYPE_WORKFLOW.started"),
  WORKFLOW_COMPLETED("$TYPE_WORKFLOW.completed"),
  WORKFLOW_CANCELED("$TYPE_WORKFLOW.canceled"),
  WORKFLOW_SIGNALED("$TYPE_WORKFLOW.signaled"),
  WORKFLOW_TASKS_RETRY_REQUESTED("$TYPE_WORKFLOW.tasks.retryRequested"),
  WORKFLOW_TASKS_RETRIED("$TYPE_WORKFLOW.tasks.retried"),

  // workflow task executor
  WORKFLOW_EXECUTOR_DISPATCHED("$TYPE_WORKFLOW_EXECUTOR.dispatched"),
  WORKFLOW_EXECUTOR_COMPLETED("$TYPE_WORKFLOW_EXECUTOR.completed"),
  WORKFLOW_EXECUTOR_FAILED("$TYPE_WORKFLOW_EXECUTOR.failed"),
  WORKFLOW_EXECUTOR_RETRY_REQUESTED("$TYPE_WORKFLOW_EXECUTOR.retryRequested"),
  WORKFLOW_EXECUTOR_RETRIED("$TYPE_WORKFLOW_EXECUTOR.retried"),

  // workflow method level
  WORKFLOW_METHOD_DISPATCHED("$TYPE_WORKFLOW_METHOD.dispatched"),
  WORKFLOW_METHOD_STARTED("$TYPE_WORKFLOW_METHOD.started"),
  WORKFLOW_METHOD_COMPLETED("$TYPE_WORKFLOW_METHOD.completed"),
  WORKFLOW_METHOD_FAILED("$TYPE_WORKFLOW_METHOD.failed"),
  WORKFLOW_METHOD_TIMED_OUT("$TYPE_WORKFLOW_METHOD.timedOut"),
  WORKFLOW_METHOD_CANCELED("$TYPE_WORKFLOW_METHOD.canceled"),

  // tasks in workflow methods
  WORKFLOW_METHOD_TASK_DISPATCHED("$TYPE_WORKFLOW_METHOD_TASK.dispatched"),
  WORKFLOW_METHOD_TASK_COMPLETED("$TYPE_WORKFLOW_METHOD_TASK.completed"),
  WORKFLOW_METHOD_TASK_FAILED("$TYPE_WORKFLOW_METHOD_TASK.failed"),
  WORKFLOW_METHOD_TASK_CANCELED("$TYPE_WORKFLOW_METHOD_TASK.canceled"),
  WORKFLOW_METHOD_TASK_TIMED_OUT("$TYPE_WORKFLOW_METHOD_TASK.timedOut"),

  // timers in workflow methods
  WORKFLOW_METHOD_TIMER_DISPATCHED("$TYPE_WORKFLOW_METHOD_TIMER.dispatched"),
  WORKFLOW_METHOD_TIMER_COMPLETED("$TYPE_WORKFLOW_METHOD_TIMER.completed"),

  // child workflows in workflow methods
  WORKFLOW_METHOD_CHILD_DISPATCHED("$TYPE_WORKFLOW_METHOD_CHILD.dispatched"),
  WORKFLOW_METHOD_CHILD_COMPLETED("$TYPE_WORKFLOW_METHOD_CHILD.completed"),
  WORKFLOW_METHOD_CHILD_FAILED("$TYPE_WORKFLOW_METHOD_CHILD.failed"),
  WORKFLOW_METHOD_CHILD_CANCELED("$TYPE_WORKFLOW_METHOD_CHILD.canceled"),
  WORKFLOW_METHOD_CHILD_TIMED_OUT("$TYPE_WORKFLOW_METHOD_CHILD.timedOut"),
  WORKFLOW_METHOD_CHILD_UNKNOWN("$TYPE_WORKFLOW_METHOD_CHILD.unknown"),
}
