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
import io.infinitic.common.utils.toJson
import io.infinitic.common.workflows.engine.messages.ChildMethodCanceled
import io.infinitic.common.workflows.engine.messages.ChildMethodCompleted
import io.infinitic.common.workflows.engine.messages.ChildMethodFailed
import io.infinitic.common.workflows.engine.messages.ChildMethodTimedOut
import io.infinitic.common.workflows.engine.messages.ChildMethodUnknown
import io.infinitic.common.workflows.engine.messages.TaskCanceled
import io.infinitic.common.workflows.engine.messages.TaskCompleted
import io.infinitic.common.workflows.engine.messages.TaskFailed
import io.infinitic.common.workflows.engine.messages.TaskTimedOut
import io.infinitic.common.workflows.engine.messages.TimerCompleted
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.events.errors.toJson
import io.infinitic.events.properties.CHILD_WORKFLOW_COMPLETED
import io.infinitic.events.properties.INFINITIC_VERSION
import io.infinitic.events.properties.RESULT
import io.infinitic.events.properties.SERVICE_NAME
import io.infinitic.events.properties.TASK_COMPLETED
import io.infinitic.events.properties.TASK_ID
import io.infinitic.events.properties.TIMER_ID
import io.infinitic.events.properties.WORKER_NAME
import io.infinitic.events.properties.WORKFLOW_ID
import io.infinitic.events.properties.WORKFLOW_METHOD_ID
import io.infinitic.events.properties.WORKFLOW_NAME
import io.infinitic.events.types.REMOTE_TASK_COMPLETED
import io.infinitic.events.types.REMOTE_TASK_FAILED
import io.infinitic.events.types.REMOTE_TASK_TIMED_OUT
import io.infinitic.events.types.REMOTE_TIMER_COMPLETED
import io.infinitic.events.types.REMOTE_WORKFLOW_CANCELED
import io.infinitic.events.types.REMOTE_WORKFLOW_COMPLETED
import io.infinitic.events.types.REMOTE_WORKFLOW_FAILED
import io.infinitic.events.types.REMOTE_WORKFLOW_TIMED_OUT
import io.infinitic.events.types.REMOTE_WORKFLOW_UNKNOWN
import io.infinitic.events.types.WORKFLOW_EXECUTOR_COMPLETED
import io.infinitic.events.types.WORKFLOW_EXECUTOR_FAILED
import kotlinx.serialization.json.JsonObject

fun WorkflowEngineMessage.workflowType(): String? = when (this) {
  is WorkflowCmdMessage -> null
  is TimerCompleted -> REMOTE_TIMER_COMPLETED
  is ChildMethodCompleted -> REMOTE_WORKFLOW_COMPLETED
  is ChildMethodCanceled -> REMOTE_WORKFLOW_CANCELED
  is ChildMethodFailed -> REMOTE_WORKFLOW_FAILED
  is ChildMethodTimedOut -> REMOTE_WORKFLOW_TIMED_OUT
  is ChildMethodUnknown -> REMOTE_WORKFLOW_UNKNOWN
  is TaskCanceled -> null
  is TaskTimedOut -> REMOTE_TASK_TIMED_OUT

  is TaskFailed -> when (isWorkflowTaskEvent()) {
    true -> WORKFLOW_EXECUTOR_FAILED
    false -> REMOTE_TASK_FAILED
  }

  is TaskCompleted -> when (isWorkflowTaskEvent()) {
    true -> WORKFLOW_EXECUTOR_COMPLETED
    false -> REMOTE_TASK_COMPLETED
  }
}

fun WorkflowEngineMessage.toJson(): JsonObject = when (this) {

  is WorkflowCmdMessage -> thisShouldNotHappen()

  is ChildMethodCompleted -> JsonObject(
      mapOf(
          CHILD_WORKFLOW_COMPLETED to JsonObject(
              with(childWorkflowReturnValue) {
                mapOf(
                    RESULT to returnValue.toJson(),
                    WORKFLOW_ID to workflowId.toJson(),
                    WORKFLOW_NAME to workflowName.toJson(),
                    WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
                )
              },
          ),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is ChildMethodCanceled -> JsonObject(
      mapOf(
          childMethodCanceledError.toJson(),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is ChildMethodFailed -> JsonObject(
      mapOf(
          childMethodFailedError.toJson(),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is ChildMethodTimedOut -> JsonObject(
      mapOf(
          childMethodTimedOutError.toJson(),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is ChildMethodUnknown -> JsonObject(
      mapOf(
          childMethodUnknownError.toJson(),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is TaskCompleted -> JsonObject(
      mapOf(
          TASK_COMPLETED to JsonObject(
              with(taskReturnValue) {
                mapOf(
                    RESULT to returnValue.toJson(),
                    TASK_ID to taskId.toJson(),
                    SERVICE_NAME to serviceName.toJson(),
                )
              },
          ),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is TaskCanceled -> TODO()

  is TaskFailed -> JsonObject(
      mapOf(
          taskFailedError.toJson(),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is TaskTimedOut -> JsonObject(
      mapOf(
          taskTimedOutError.toJson(),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is TimerCompleted -> JsonObject(
      mapOf(
          "timerCompleted" to JsonObject(
              mapOf(
                  TIMER_ID to timerId.toJson(),
              ),
          ),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )
}
