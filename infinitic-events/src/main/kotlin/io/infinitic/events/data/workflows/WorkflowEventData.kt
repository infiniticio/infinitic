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

import io.infinitic.common.utils.toJson
import io.infinitic.common.workflows.engine.messages.ChildMethodDispatchedEvent
import io.infinitic.common.workflows.engine.messages.MethodCanceledEvent
import io.infinitic.common.workflows.engine.messages.MethodCommandedEvent
import io.infinitic.common.workflows.engine.messages.MethodCompletedEvent
import io.infinitic.common.workflows.engine.messages.MethodFailedEvent
import io.infinitic.common.workflows.engine.messages.MethodTimedOutEvent
import io.infinitic.common.workflows.engine.messages.TaskDispatchedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCanceledEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCompletedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import io.infinitic.events.errors.toJson
import io.infinitic.events.properties.INFINITIC_VERSION
import io.infinitic.events.properties.REQUESTER
import io.infinitic.events.properties.RESULT
import io.infinitic.events.properties.SERVICE_NAME
import io.infinitic.events.properties.TASK_ARGS
import io.infinitic.events.properties.TASK_ID
import io.infinitic.events.properties.TASK_NAME
import io.infinitic.events.properties.WORKER_NAME
import io.infinitic.events.properties.WORKFLOW_ID
import io.infinitic.events.properties.WORKFLOW_METHOD_ARGS
import io.infinitic.events.properties.WORKFLOW_METHOD_ID
import io.infinitic.events.properties.WORKFLOW_METHOD_NAME
import io.infinitic.events.properties.WORKFLOW_NAME
import io.infinitic.events.types.METHOD_CANCELED
import io.infinitic.events.types.METHOD_COMMANDED
import io.infinitic.events.types.METHOD_COMPLETED
import io.infinitic.events.types.METHOD_FAILED
import io.infinitic.events.types.METHOD_TIMED_OUT
import io.infinitic.events.types.REMOTE_TASK_DISPATCHED
import io.infinitic.events.types.REMOTE_WORKFLOW_DISPATCHED
import io.infinitic.events.types.WORKFLOW_CANCELED
import io.infinitic.events.types.WORKFLOW_COMPLETED
import io.infinitic.events.types.WORKFLOW_EXECUTOR_DISPATCHED
import kotlinx.serialization.json.JsonObject

fun WorkflowEventMessage.workflowType(): String = when (this) {
  is WorkflowCompletedEvent -> WORKFLOW_COMPLETED
  is WorkflowCanceledEvent -> WORKFLOW_CANCELED
  is MethodCommandedEvent -> METHOD_COMMANDED
  is MethodCompletedEvent -> METHOD_COMPLETED
  is MethodFailedEvent -> METHOD_FAILED
  is MethodCanceledEvent -> METHOD_CANCELED
  is MethodTimedOutEvent -> METHOD_TIMED_OUT
  is ChildMethodDispatchedEvent -> REMOTE_WORKFLOW_DISPATCHED
  is TaskDispatchedEvent -> when (isWorkflowTaskEvent()) {
    true -> WORKFLOW_EXECUTOR_DISPATCHED
    false -> REMOTE_TASK_DISPATCHED
  }
}

fun WorkflowEventMessage.toJson(): JsonObject = when (this) {

  is WorkflowCompletedEvent -> JsonObject(
      mapOf(
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is WorkflowCanceledEvent -> JsonObject(
      mapOf(
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is MethodCommandedEvent -> JsonObject(
      mapOf(
          WORKFLOW_METHOD_ARGS to methodParameters.toJson(),
          WORKFLOW_METHOD_NAME to methodName.toJson(),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is MethodCompletedEvent -> JsonObject(
      mapOf(
          RESULT to returnValue.toJson(),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is MethodFailedEvent -> JsonObject(
      mapOf(
          deferredError.toJson(),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is MethodCanceledEvent -> JsonObject(
      mapOf(
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is MethodTimedOutEvent -> JsonObject(
      mapOf(
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is TaskDispatchedEvent -> JsonObject(
      mapOf(
          "taskDispatched" to JsonObject(
              with(taskDispatched) {
                mapOf(
                    SERVICE_NAME to serviceName.toJson(),
                    TASK_NAME to taskName.toJson(),
                    TASK_ARGS to methodParameters.toJson(),
                    TASK_ID to taskId.toJson(),
                )
              },
          ),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is ChildMethodDispatchedEvent -> JsonObject(
      mapOf(
          "childWorkflowDispatched" to JsonObject(
              with(childMethodDispatched) {
                mapOf(
                    WORKFLOW_NAME to workflowName.toJson(),
                    WORKFLOW_ID to workflowId.toJson(),
                    WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
                    WORKFLOW_METHOD_NAME to methodName.toJson(),
                    WORKFLOW_METHOD_ARGS to methodParameters.toJson(),
                )
              },
          ),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )
}
