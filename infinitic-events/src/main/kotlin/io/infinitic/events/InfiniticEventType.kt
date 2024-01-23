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
private const val TYPE_WORKFLOW_TASK = "$TYPE_WORKFLOW.task"
private const val TYPE_WORKFLOW_METHOD = "$TYPE_WORKFLOW.method"

private const val TYPE_TASK = "$TYPE_DOMAIN.task"

sealed interface InfiniticEventType {
  val type: String
}

/**
 * ABOUT SERVICES
 * Source = urn:pulsar:cluster-name/tenant/namespace/services/$ServiceName
 */

sealed interface InfiniticServiceEventType : InfiniticEventType {
  override val type: String
}

data object TaskDispatched : InfiniticServiceEventType {
  override val type = "$TYPE_TASK.dispatched"
}

data object TaskStarted : InfiniticServiceEventType {
  override val type = "$TYPE_TASK.started"
}

data object TaskCompleted : InfiniticServiceEventType {
  override val type = "$TYPE_TASK.completed"
}

data object TaskFailed : InfiniticServiceEventType {
  override val type = "$TYPE_TASK.failed"
}

data object TaskRetried : InfiniticServiceEventType {
  override val type = "$TYPE_TASK.retried"
}

/**
 * ABOUT WORKFLOWS
 * Source = urn:pulsar:cluster-name/tenant/namespace/workflow/$WorkflowName
 */

sealed interface InfiniticWorkflowEventType : InfiniticEventType {
  override val type: String
}

data object WorkflowDispatched : InfiniticWorkflowEventType {
  override val type = "$TYPE_WORKFLOW.dispatched"
}

data object WorkflowStarted : InfiniticWorkflowEventType {
  override val type = "$TYPE_WORKFLOW.started"
}

data object WorkflowCancelRequested : InfiniticWorkflowEventType {
  override val type = "$TYPE_WORKFLOW.cancelRequested"
}

data object WorkflowCanceled : InfiniticWorkflowEventType {
  override val type = "$TYPE_WORKFLOW.canceled"
}

data object WorkflowSignalSent : InfiniticWorkflowEventType {
  override val type = "$TYPE_WORKFLOW.signalSent"
}

data object WorkflowSignalReceived : InfiniticWorkflowEventType {
  override val type = "$TYPE_WORKFLOW.signalReceived"
}

data object WorkflowCompleted : InfiniticWorkflowEventType {
  override val type = "$TYPE_WORKFLOW.completed"
}

data object TaskRetryRequested : InfiniticWorkflowTaskEventType {
  override val type = "$TYPE_WORKFLOW.retryTaskRequested"
}


/**
 * ABOUT WORKFLOW TASKS
 * Source = urn:pulsar:cluster-name/tenant/namespace/workflow/$WorkflowName
 */

sealed interface InfiniticWorkflowTaskEventType : InfiniticWorkflowEventType {
  override val type: String
}

data object WorkflowTaskRetryRequested : InfiniticWorkflowTaskEventType {
  override val type = "$TYPE_WORKFLOW_TASK.retryRequested"
}

data object WorkflowTaskRetried : InfiniticWorkflowTaskEventType {
  override val type = "$TYPE_WORKFLOW_TASK.retried"
}

data object WorkflowTaskDispatched : InfiniticWorkflowTaskEventType {
  override val type = "$TYPE_WORKFLOW_TASK.dispatched"
}

data object WorkflowTaskCompleted : InfiniticWorkflowTaskEventType {
  override val type = "$TYPE_WORKFLOW_TASK.completed"
}

data object WorkflowTaskFailed : InfiniticWorkflowTaskEventType {
  override val type = "$TYPE_WORKFLOW_TASK.failed"
}

/**
 * ABOUT WORKFLOW METHODS
 */

// Happening at Workflow Method Level (Point of view of Workflow Method)
// Source = urn:pulsar:cluster-name/tenant/namespace/workflows/$workflowName/$methodName
// Source = urn:pulsar:cluster-name/tenant/namespace/workflows/$workflowName/$workflowId/$methodName

sealed interface InfiniticWorkflowMethodEventType : InfiniticWorkflowEventType {
  override val type: String
}

data object WorkflowMethodDispatched : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.dispatched"
}

data object WorkflowMethodStarted : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.started"
}

data object WorkflowMethodCompleted : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.completed"
}

data object WorkflowMethodFailed : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.failed"
}

data object WorkflowMethodCanceled : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.canceled"
}

data object WorkflowMethodTimedOut : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.timedOut"
}

/**
 * ABOUT TIMERS DISPATCHED FROM A WORKFLOW METHOD
 */

data object WorkflowMethodTimerDispatched : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.timer.dispatched"
}

data object WorkflowMethodTimerCompleted : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.timer.completed"
}

/**
 * ABOUT TASKS DISPATCHED FROM A WORKFLOW METHOD
 */
data object WorkflowMethodTaskDispatched : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.task.dispatched"
}

data object WorkflowMethodTaskCompleted : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.task.completed"
}

data object WorkflowMethodTaskFailed : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.task.failed"
}

data object WorkflowMethodTaskCanceled : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.task.canceled"
}

data object WorkflowMethodTaskTimedOut : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.task.timedout"
}


/**
 * ABOUT CHILD WORKFLOW METHOD DISPATCHED FROM A WORKFLOW METHOD
 */

data object WorkflowMethodChildDispatched : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.child.dispatched"
}

data object WorkflowMethodChildCompleted : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.child.completed"
}

data object WorkflowMethodChildFailed : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.child.failed"
}

data object WorkflowMethodChildCanceled : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.child.canceled"
}

data object WorkflowMethodChildTimedOut : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.child.timedout"
}
