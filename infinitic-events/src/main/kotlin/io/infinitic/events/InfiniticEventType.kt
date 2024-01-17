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
private const val TYPE_TASK = "$TYPE_DOMAIN.task"
private const val TYPE_WORKFLOW = "$TYPE_DOMAIN.workflow"
private const val TYPE_WORKFLOW_METHOD = "$TYPE_WORKFLOW.method"

enum class InfiniticEventType(val type: String) {

  // Happening at Workflow Level
  // Source = urn:pulsar:cluster-name/tenant/namespace/workflows/$workflowName

  WORKFLOW_REQUESTED(
      type = "$TYPE_WORKFLOW.requested",
  ),
  WORKFLOW_STARTED(
      type = "$TYPE_WORKFLOW.started",
  ),
  WORKFLOW_CANCEL_REQUESTED(
      type = "$TYPE_WORKFLOW.cancel.requested",
  ),
  WORKFLOW_CANCELED(
      type = "$TYPE_WORKFLOW.canceled",
  ),
  WORKFLOW_SIGNAL_SENT(
      type = "$TYPE_WORKFLOW.signal",
  ),
  WORKFLOW_SIGNAL_RECEIVED(
      type = "$TYPE_WORKFLOW.signaled",
  ),
  WORKFLOW_COMPLETED(
      type = "$TYPE_WORKFLOW.completed",
  ),
  WORKFLOW_TASK_RETRY_REQUESTED(
      type = "$TYPE_WORKFLOW.task.retry",
  ),
  WORKFLOW_TASK_RETRIED(
      type = "$TYPE_WORKFLOW.task.retried",
  ),
  WORKFLOW_TASK_DISPATCHED(
      type = "$TYPE_WORKFLOW.task.dispatched",
  ),
  WORKFLOW_TASK_COMPLETED(
      type = "$TYPE_WORKFLOW.task.completed",
  ),
  WORKFLOW_TASK_FAILED(
      type = "$TYPE_WORKFLOW.task.failed",
  ),
  WORKFLOW_TASK_TIMED_OUT(
      type = "$TYPE_WORKFLOW.task.timedout",
  ),

  // Happening at Workflow Method Level (Point of view of Workflow Method)
  // Source = urn:pulsar:cluster-name/tenant/namespace/workflows/$workflowName/$methodName
  // Source = urn:pulsar:cluster-name/tenant/namespace/workflows/$workflowName/$workflowId/$methodName

  METHOD_REQUESTED(
      type = "$TYPE_WORKFLOW_METHOD.requested",
  ),
  METHOD_STARTED(
      type = "$TYPE_WORKFLOW_METHOD.started",
  ),
  METHOD_COMPLETED(
      type = "$TYPE_WORKFLOW_METHOD.completed",
  ),
  METHOD_FAILED(
      type = "$TYPE_WORKFLOW_METHOD.failed",
  ),
  METHOD_CANCELED(
      type = "$TYPE_WORKFLOW_METHOD.canceled",
  ),
  METHOD_TIMED_OUT(
      type = "$TYPE_WORKFLOW_METHOD.timedOut",
  ),

  // Inside workflow method

  METHOD_TIMER_DISPATCHED(
      type = "$TYPE_WORKFLOW_METHOD.timer.dispatched",
  ),
  METHOD_TIMER_COMPLETED(
      type = "$TYPE_WORKFLOW_METHOD.timer.completed",
  ),
  METHOD_TASK_DISPATCHED(
      type = "$TYPE_WORKFLOW_METHOD.task.dispatched",
  ),
  METHOD_TASK_COMPLETED(
      type = "$TYPE_WORKFLOW_METHOD.task.completed",
  ),
  METHOD_TASK_FAILED(
      type = "$TYPE_WORKFLOW_METHOD.task.failed",
  ),
  METHOD_TASK_TIMED_OUT(
      type = "$TYPE_WORKFLOW_METHOD.task.timedout",
  ),
  METHOD_CHILD_DISPATCHED(
      type = "$TYPE_WORKFLOW_METHOD.child.dispatched",
  ),
  METHOD_CHILD_COMPLETED(
      type = "$TYPE_WORKFLOW_METHOD.child.completed",
  ),
  METHOD_CHILD_FAILED(
      type = "$TYPE_WORKFLOW_METHOD.child.failed",
  ),
  METHOD_CHILD_TIMED_OUT(
      type = "$TYPE_WORKFLOW_METHOD.child.timedout",
  ),

  // Happening at Task Level (Point of view of Task)
  // Source = urn:pulsar:cluster-name/tenant/namespace/tasks/serviceName

  TASK_REQUESTED(
      type = "$TYPE_TASK.task.requested",
  ),
  TASK_STARTED(
      type = "$TYPE_TASK.task.started",
  ),
  TASK_COMPLETED(
      type = "$TYPE_TASK.task.completed",
  ),
  TASK_FAILED(
      type = "$TYPE_TASK.task.failed",
  ),
  TASK_RETRIED(
      type = "$TYPE_TASK.task.retried",
  ),
}
