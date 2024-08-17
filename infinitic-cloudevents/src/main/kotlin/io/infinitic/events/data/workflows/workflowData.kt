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

import io.infinitic.cloudEvents.CHANNEL_NAME
import io.infinitic.cloudEvents.INFINITIC_VERSION
import io.infinitic.cloudEvents.METHOD_ARGS
import io.infinitic.cloudEvents.METHOD_ID
import io.infinitic.cloudEvents.METHOD_NAME
import io.infinitic.cloudEvents.OUTPUT
import io.infinitic.cloudEvents.REQUESTER
import io.infinitic.cloudEvents.SERVICE_NAME
import io.infinitic.cloudEvents.SIGNAL_DATA
import io.infinitic.cloudEvents.SIGNAL_ID
import io.infinitic.cloudEvents.TASK_ID
import io.infinitic.cloudEvents.TASK_STATUS
import io.infinitic.cloudEvents.TIMER_ID
import io.infinitic.cloudEvents.WORKER_NAME
import io.infinitic.cloudEvents.WORKFLOW_ID
import io.infinitic.cloudEvents.WORKFLOW_META
import io.infinitic.cloudEvents.WORKFLOW_NAME
import io.infinitic.cloudEvents.WORKFLOW_TAGS
import io.infinitic.cloudEvents.WORKFLOW_VERSION
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
import io.infinitic.common.workflows.engine.messages.RemoteTaskFailed
import io.infinitic.common.workflows.engine.messages.RemoteTaskTimedOut
import io.infinitic.common.workflows.engine.messages.RemoteTimerCompleted
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.SignalDiscardedEvent
import io.infinitic.common.workflows.engine.messages.SignalDispatchedEvent
import io.infinitic.common.workflows.engine.messages.SignalReceivedEvent
import io.infinitic.common.workflows.engine.messages.TaskDispatchedEvent
import io.infinitic.common.workflows.engine.messages.TimerDispatchedEvent
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowCanceledEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowCompletedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.events.errors.toJson
import io.infinitic.events.types.REMOTE_METHOD_DISPATCHED
import io.infinitic.events.types.SIGNAL_DISCARDED
import io.infinitic.events.types.SIGNAL_DISPATCHED
import io.infinitic.events.types.SIGNAL_RECEIVED
import io.infinitic.events.types.TIMER_DISPATCHED
import kotlinx.serialization.json.JsonObject

fun WorkflowCmdMessage.toJson(): JsonObject = when (this) {
  is DispatchWorkflow -> JsonObject(
      mapOf(
          workflowSimpleType()!! to JsonObject(
              mapOf(
                  WORKFLOW_META to workflowMeta.toJson(),
                  WORKFLOW_TAGS to workflowTags.toJson(),
              ),
          ),
          WORKFLOW_NAME to workflowName.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is DispatchMethod -> JsonObject(
      mapOf(
          workflowSimpleType()!! to JsonObject(
              mapOf(
                  METHOD_ARGS to methodParameters.toJson(),
                  METHOD_NAME to workflowMethodName.toJson(),
                  METHOD_ID to workflowMethodId.toJson(),
              ),
          ),
          WORKFLOW_NAME to workflowName.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is CancelWorkflow -> JsonObject(
      when (workflowMethodId) {
        null -> mapOf(
            WORKFLOW_NAME to workflowName.toJson(),
            REQUESTER to requester.toJson(),
            INFINITIC_VERSION to infiniticVersion.toJson(),
        )

        else -> mapOf(
            workflowSimpleType()!! to JsonObject(
                mapOf(
                    METHOD_ID to workflowMethodId.toJson(),
                ),
            ),
            WORKFLOW_NAME to workflowName.toJson(),
            REQUESTER to requester.toJson(),
            INFINITIC_VERSION to infiniticVersion.toJson(),
        )
      },
  )

  is CompleteTimers -> TODO()

  is CompleteWorkflow -> TODO()

  is RetryTasks -> JsonObject(
      mapOf(
          workflowSimpleType()!! to JsonObject(
              mapOf(
                  TASK_ID to taskId.toJson(),
                  TASK_STATUS to taskStatus.toJson(),
                  SERVICE_NAME to serviceName.toJson(),
              ),
          ),
          WORKFLOW_NAME to workflowName.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is RetryWorkflowTask -> JsonObject(
      mapOf(
          WORKFLOW_NAME to workflowName.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is SendSignal -> JsonObject(
      mapOf(
          workflowSimpleType()!! to JsonObject(
              mapOf(
                  CHANNEL_NAME to channelName.toJson(),
                  SIGNAL_ID to signalId.toJson(),
                  SIGNAL_DATA to signalData.toJson(),
              ),
          ),
          WORKFLOW_NAME to workflowName.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is WaitWorkflow -> thisShouldNotHappen()
}

fun WorkflowStateEngineMessage.toJson(): JsonObject = when (this) {

  is WorkflowCmdMessage -> thisShouldNotHappen()

  is RemoteMethodCompleted -> JsonObject(
      mapOf(
          workflowSimpleType()!! to JsonObject(
              with(childWorkflowReturnValue) {
                mapOf(
                    OUTPUT to returnValue.toJson(),
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
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is RemoteMethodCanceled -> JsonObject(
      mapOf(
          childMethodCanceledError.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is RemoteMethodFailed -> JsonObject(
      mapOf(
          childMethodFailedError.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is RemoteMethodTimedOut -> JsonObject(
      mapOf(
          childMethodTimedOutError.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is RemoteMethodUnknown -> JsonObject(
      mapOf(
          childMethodUnknownError.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is RemoteTaskCompleted -> JsonObject(
      mapOf(
          workflowSimpleType()!! to taskReturnValue.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is RemoteTaskCanceled -> TODO()

  is RemoteTaskFailed -> JsonObject(
      mapOf(
          taskFailedError.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is RemoteTaskTimedOut -> JsonObject(
      mapOf(
          taskTimedOutError.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is RemoteTimerCompleted -> JsonObject(
      mapOf(
          workflowSimpleType()!! to JsonObject(
              mapOf(
                  TIMER_ID to timerId.toJson(),
              ),
          ),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )
}

fun WorkflowEventMessage.toJson(): JsonObject = when (this) {

  is WorkflowCompletedEvent -> JsonObject(
      mapOf(
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is WorkflowCanceledEvent -> JsonObject(
      mapOf(
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is MethodCommandedEvent -> JsonObject(
      mapOf(
          workflowSimpleType() to JsonObject(
              mapOf(
                  METHOD_ARGS to methodParameters.toJson(),
                  METHOD_NAME to methodName.toJson(),
                  METHOD_ID to workflowMethodId.toJson(),
              ),
          ),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is MethodCompletedEvent -> JsonObject(
      mapOf(
          workflowSimpleType() to JsonObject(
              mapOf(
                  OUTPUT to returnValue.toJson(),
                  METHOD_ID to workflowMethodId.toJson(),
                  METHOD_NAME to workflowMethodName.toJson(),
              ),
          ),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is MethodFailedEvent -> JsonObject(
      mapOf(
          workflowSimpleType() to JsonObject(
              mapOf(
                  deferredError.toJson(),
                  METHOD_ID to workflowMethodId.toJson(),
                  METHOD_NAME to workflowMethodName.toJson(),
              ),
          ),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is MethodCanceledEvent -> JsonObject(
      mapOf(
          workflowSimpleType() to JsonObject(
              mapOf(
                  METHOD_ID to workflowMethodId.toJson(),
                  METHOD_NAME to workflowMethodName.toJson(),
              ),
          ),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is MethodTimedOutEvent -> JsonObject(
      mapOf(
          workflowSimpleType() to JsonObject(
              mapOf(
                  METHOD_ID to workflowMethodId.toJson(),
                  METHOD_NAME to workflowMethodName.toJson(),
              ),
          ),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is TaskDispatchedEvent -> JsonObject(
      mapOf(
          workflowSimpleType() to taskDispatched.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is RemoteMethodDispatchedEvent -> JsonObject(
      mapOf(
          REMOTE_METHOD_DISPATCHED to remoteMethodDispatched.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is TimerDispatchedEvent -> JsonObject(
      mapOf(
          TIMER_DISPATCHED to timerDispatched.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is SignalDispatchedEvent -> JsonObject(
      mapOf(
          SIGNAL_DISPATCHED to remoteSignalDispatched.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is SignalDiscardedEvent -> JsonObject(
      mapOf(
          SIGNAL_DISCARDED to signalDiscarded.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )

  is SignalReceivedEvent -> JsonObject(
      mapOf(
          SIGNAL_RECEIVED to signalReceived.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_VERSION to workflowVersion.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to infiniticVersion.toJson(),
      ),
  )
}
