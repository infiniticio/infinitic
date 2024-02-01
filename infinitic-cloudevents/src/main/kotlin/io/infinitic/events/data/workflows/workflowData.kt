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
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.CompleteWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.MethodCanceledEvent
import io.infinitic.common.workflows.engine.messages.MethodCommandedEvent
import io.infinitic.common.workflows.engine.messages.MethodCompletedEvent
import io.infinitic.common.workflows.engine.messages.MethodFailedEvent
import io.infinitic.common.workflows.engine.messages.MethodTimedOutEvent
import io.infinitic.common.workflows.engine.messages.RemoteMethodCanceled
import io.infinitic.common.workflows.engine.messages.RemoteMethodCompleted
import io.infinitic.common.workflows.engine.messages.RemoteMethodDispatchedEvent
import io.infinitic.common.workflows.engine.messages.RemoteMethodFailed
import io.infinitic.common.workflows.engine.messages.RemoteMethodTimedOut
import io.infinitic.common.workflows.engine.messages.RemoteMethodUnknown
import io.infinitic.common.workflows.engine.messages.RemoteTaskCanceled
import io.infinitic.common.workflows.engine.messages.RemoteTaskCompleted
import io.infinitic.common.workflows.engine.messages.RemoteTaskDispatchedEvent
import io.infinitic.common.workflows.engine.messages.RemoteTaskFailed
import io.infinitic.common.workflows.engine.messages.RemoteTaskTimedOut
import io.infinitic.common.workflows.engine.messages.RemoteTimerCompleted
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowCanceledEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowCompletedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import io.infinitic.events.errors.toJson
import io.infinitic.events.properties.CHANNEL_NAME
import io.infinitic.events.properties.INFINITIC_VERSION
import io.infinitic.events.properties.METHOD_ARGS
import io.infinitic.events.properties.METHOD_ID
import io.infinitic.events.properties.METHOD_NAME
import io.infinitic.events.properties.REQUESTER
import io.infinitic.events.properties.RESULT
import io.infinitic.events.properties.SERVICE_NAME
import io.infinitic.events.properties.SIGNAL_DATA
import io.infinitic.events.properties.SIGNAL_ID
import io.infinitic.events.properties.TASK_ARGS
import io.infinitic.events.properties.TASK_ID
import io.infinitic.events.properties.TASK_NAME
import io.infinitic.events.properties.TASK_STATUS
import io.infinitic.events.properties.TIMER_ID
import io.infinitic.events.properties.WORKER_NAME
import io.infinitic.events.properties.WORKFLOW_ID
import io.infinitic.events.properties.WORKFLOW_META
import io.infinitic.events.properties.WORKFLOW_NAME
import io.infinitic.events.properties.WORKFLOW_TAGS
import io.infinitic.events.types.REMOTE_METHOD_COMPLETED
import io.infinitic.events.types.REMOTE_METHOD_DISPATCHED
import io.infinitic.events.types.REMOTE_TASK_COMPLETED
import io.infinitic.events.types.REMOTE_TASK_DISPATCHED
import io.infinitic.events.types.REMOTE_TIMER_COMPLETED
import kotlinx.serialization.json.JsonObject

fun WorkflowCmdMessage.toJson(): JsonObject = when (this) {
  is DispatchWorkflow -> JsonObject(
      mapOf(
          WORKFLOW_META to workflowMeta.toJson(),
          WORKFLOW_TAGS to workflowTags.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is DispatchMethod -> JsonObject(
      mapOf(
          METHOD_ARGS to methodParameters.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is CancelWorkflow -> JsonObject(
      when (workflowMethodId) {
        null -> mapOf(
            WORKFLOW_NAME to workflowName.toJson(),
            REQUESTER to requester.toJson(),
            INFINITIC_VERSION to version.toJson(),
        )

        else -> mapOf(
            METHOD_ID to workflowMethodId.toJson(),
            WORKFLOW_NAME to workflowName.toJson(),
            REQUESTER to requester.toJson(),
            INFINITIC_VERSION to version.toJson(),
        )
      },
  )

  is CompleteTimers -> TODO()

  is CompleteWorkflow -> TODO()

  is RetryTasks -> JsonObject(
      mapOf(
          TASK_ID to taskId.toJson(),
          TASK_STATUS to taskStatus.toJson(),
          SERVICE_NAME to serviceName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is RetryWorkflowTask -> JsonObject(
      mapOf(
          WORKFLOW_NAME to workflowName.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is SendSignal -> JsonObject(
      mapOf(
          CHANNEL_NAME to channelName.toJson(),
          SIGNAL_ID to signalId.toJson(),
          SIGNAL_DATA to signalData.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is WaitWorkflow -> thisShouldNotHappen()
}

fun WorkflowEngineMessage.toJson(): JsonObject = when (this) {

  is WorkflowCmdMessage -> thisShouldNotHappen()

  is RemoteMethodCompleted -> JsonObject(
      mapOf(
          REMOTE_METHOD_COMPLETED to JsonObject(
              with(childWorkflowReturnValue) {
                mapOf(
                    RESULT to returnValue.toJson(),
                    WORKFLOW_ID to workflowId.toJson(),
                    WORKFLOW_NAME to workflowName.toJson(),
                    METHOD_ID to workflowMethodId.toJson(),
                    METHOD_NAME to workflowMethodName.toJson(),
                )
              },
          ),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is RemoteMethodCanceled -> JsonObject(
      mapOf(
          childMethodCanceledError.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is RemoteMethodFailed -> JsonObject(
      mapOf(
          childMethodFailedError.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is RemoteMethodTimedOut -> JsonObject(
      mapOf(
          childMethodTimedOutError.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is RemoteMethodUnknown -> JsonObject(
      mapOf(
          childMethodUnknownError.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is RemoteTaskCompleted -> JsonObject(
      mapOf(
          REMOTE_TASK_COMPLETED to JsonObject(
              with(taskReturnValue) {
                mapOf(
                    RESULT to returnValue.toJson(),
                    TASK_ID to taskId.toJson(),
                    TASK_NAME to methodName.toJson(),
                    SERVICE_NAME to serviceName.toJson(),
                )
              },
          ),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is RemoteTaskCanceled -> TODO()

  is RemoteTaskFailed -> JsonObject(
      mapOf(
          taskFailedError.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is RemoteTaskTimedOut -> JsonObject(
      mapOf(
          taskTimedOutError.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is RemoteTimerCompleted -> JsonObject(
      mapOf(
          REMOTE_TIMER_COMPLETED to JsonObject(
              mapOf(
                  TIMER_ID to timerId.toJson(),
              ),
          ),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )
}

fun WorkflowEventMessage.toJson(): JsonObject = when (this) {

  is WorkflowCompletedEvent -> JsonObject(
      mapOf(
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is WorkflowCanceledEvent -> JsonObject(
      mapOf(
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is MethodCommandedEvent -> JsonObject(
      mapOf(
          METHOD_ARGS to methodParameters.toJson(),
          METHOD_NAME to methodName.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is MethodCompletedEvent -> JsonObject(
      mapOf(
          RESULT to returnValue.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is MethodFailedEvent -> JsonObject(
      mapOf(
          deferredError.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is MethodCanceledEvent -> JsonObject(
      mapOf(
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is MethodTimedOutEvent -> JsonObject(
      mapOf(
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is RemoteTaskDispatchedEvent -> JsonObject(
      mapOf(
          REMOTE_TASK_DISPATCHED to JsonObject(
              with(remoteTaskDispatched) {
                mapOf(
                    SERVICE_NAME to serviceName.toJson(),
                    TASK_NAME to taskName.toJson(),
                    TASK_ARGS to methodParameters.toJson(),
                    TASK_ID to taskId.toJson(),
                )
              },
          ),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is RemoteMethodDispatchedEvent -> JsonObject(
      mapOf(
          REMOTE_METHOD_DISPATCHED to JsonObject(
              with(remoteMethodDispatched) {
                mapOf(
                    METHOD_ARGS to methodParameters.toJson(),
                    WORKFLOW_ID to workflowId.toJson(),
                    WORKFLOW_NAME to workflowName.toJson(),
                    METHOD_ID to workflowMethodId.toJson(),
                    METHOD_NAME to methodName.toJson(),
                )
              },
          ),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )
}
