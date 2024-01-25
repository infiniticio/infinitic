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
private const val TYPE_WORKFLOW_WORKFLOW_TASK = "$TYPE_WORKFLOW.workflowTask"
private const val TYPE_WORKFLOW_METHOD = "$TYPE_WORKFLOW.method"
private const val TYPE_WORKFLOW_METHOD_CHILD = "$TYPE_WORKFLOW_METHOD.child"
private const val TYPE_WORKFLOW_METHOD_TASK = "$TYPE_WORKFLOW_METHOD.task"

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

data object TaskDispatchedType : InfiniticServiceEventType {
  override val type = "$TYPE_TASK.dispatched"
}

data object TaskStartedType : InfiniticServiceEventType {
  override val type = "$TYPE_TASK.started"
}

data object TaskCompletedType : InfiniticServiceEventType {
  override val type = "$TYPE_TASK.completed"
}

data object TaskFailedType : InfiniticServiceEventType {
  override val type = "$TYPE_TASK.failed"
}

data object TaskRetriedType : InfiniticServiceEventType {
  override val type = "$TYPE_TASK.retried"
}

/**
 * ABOUT WORKFLOWS
 * Source = urn:pulsar:cluster-name/tenant/namespace/workflow/$WorkflowName
 */

sealed interface InfiniticWorkflowEventType : InfiniticEventType {
  override val type: String
}

data object WorkflowDispatchedType : InfiniticWorkflowEventType {
  override val type = "$TYPE_WORKFLOW.dispatched"
}

data object WorkflowStartedType : InfiniticWorkflowEventType {
  override val type = "$TYPE_WORKFLOW.started"
}

data object WorkflowCancelRequestedType : InfiniticWorkflowEventType {
  override val type = "$TYPE_WORKFLOW.cancelRequested"
}

data object WorkflowCanceledType : InfiniticWorkflowEventType {
  override val type = "$TYPE_WORKFLOW.canceled"
}

data object WorkflowSignalSentType : InfiniticWorkflowEventType {
  override val type = "$TYPE_WORKFLOW.signalSent"
}

data object WorkflowSignalReceivedType : InfiniticWorkflowEventType {
  override val type = "$TYPE_WORKFLOW.signalReceived"
}

data object WorkflowCompletedType : InfiniticWorkflowEventType {
  override val type = "$TYPE_WORKFLOW.completed"
}

data object TaskRetryRequestedType : InfiniticWorkflowTaskEventType {
  override val type = "$TYPE_WORKFLOW.retryTaskRequested"
}


/**
 * ABOUT WORKFLOW TASKS
 * Source = urn:pulsar:cluster-name/tenant/namespace/workflow/$WorkflowName
 */

sealed interface InfiniticWorkflowTaskEventType : InfiniticWorkflowEventType {
  override val type: String
}

data object WorkflowTaskRetryRequestedType : InfiniticWorkflowTaskEventType {
  override val type = "$TYPE_WORKFLOW_WORKFLOW_TASK.retryRequested"
}

data object WorkflowTaskRetriedType : InfiniticWorkflowTaskEventType {
  override val type = "$TYPE_WORKFLOW_WORKFLOW_TASK.retried"
}

data object WorkflowTaskDispatchedType : InfiniticWorkflowTaskEventType {
  override val type = "$TYPE_WORKFLOW_WORKFLOW_TASK.dispatched"
}

data object WorkflowTaskCompletedType : InfiniticWorkflowTaskEventType {
  override val type = "$TYPE_WORKFLOW_WORKFLOW_TASK.completed"
}

data object WorkflowTaskFailedType : InfiniticWorkflowTaskEventType {
  override val type = "$TYPE_WORKFLOW_WORKFLOW_TASK.failed"
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

data object WorkflowMethodDispatchedType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.dispatched"
}

data object WorkflowMethodStartedType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.started"
}

data object WorkflowMethodCompletedType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.completed"
}

data object WorkflowMethodFailedType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.failed"
}

data object WorkflowMethodCanceledType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.canceled"
}

data object WorkflowMethodTimedOutType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.timedOut"
}

/**
 * ABOUT TIMERS DISPATCHED FROM A WORKFLOW METHOD
 */

data object WorkflowMethodTimerDispatchedType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.timer.dispatched"
}

data object ChildTimerCompletedType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD.timer.completed"
}

/**
 * ABOUT TASKS DISPATCHED FROM A WORKFLOW METHOD
 */
data object ChildTaskDispatchedType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD_TASK.dispatched"
}

data object ChildTaskCompletedType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD_TASK.completed"
}

data object ChildTaskFailedType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD_TASK.failed"
}

data object ChildTaskCanceledType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD_TASK.canceled"
}

data object ChildTaskTimedOutType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD_TASK.timedout"
}


/**
 * ABOUT CHILD WORKFLOW METHOD DISPATCHED FROM A WORKFLOW METHOD
 */

data object ChildMethodDispatchedType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD_CHILD.dispatched"
}

data object ChildMethodCompletedType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD_CHILD.completed"
}

data object ChildMethodFailedType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD_CHILD.failed"
}

data object ChildMethodCanceledType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD_CHILD.canceled"
}

data object ChildMethodTimedOutType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD_CHILD.timedout"
}

data object ChildMethodUnknownType : InfiniticWorkflowMethodEventType {
  override val type = "$TYPE_WORKFLOW_METHOD_CHILD.unknown"
}
