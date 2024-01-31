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
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.events.properties.CHANNEL_NAME
import io.infinitic.events.properties.INFINITIC_VERSION
import io.infinitic.events.properties.REQUESTER
import io.infinitic.events.properties.SERVICE_NAME
import io.infinitic.events.properties.SIGNAL_DATA
import io.infinitic.events.properties.SIGNAL_ID
import io.infinitic.events.properties.TARGET
import io.infinitic.events.properties.TASK_ID
import io.infinitic.events.properties.TASK_STATUS
import io.infinitic.events.properties.WORKFLOW_META
import io.infinitic.events.properties.WORKFLOW_METHOD_ARGS
import io.infinitic.events.properties.WORKFLOW_METHOD_ID
import io.infinitic.events.properties.WORKFLOW_METHOD_NAME
import io.infinitic.events.properties.WORKFLOW_TAGS
import io.infinitic.events.types.WORKFLOW_CANCELED
import io.infinitic.events.types.WORKFLOW_COMMANDED
import io.infinitic.events.types.WORKFLOW_EXECUTOR_RETRY_COMMANDED
import io.infinitic.events.types.WORKFLOW_METHOD_COMMANDED
import io.infinitic.events.types.WORKFLOW_SIGNAL_COMMANDED
import io.infinitic.events.types.WORKFLOW_TASKS_RETRY_COMMANDED
import kotlinx.serialization.json.JsonObject

fun WorkflowCmdMessage.workflowType(): String? = when (this) {
  is DispatchWorkflow -> WORKFLOW_COMMANDED
  is DispatchMethod -> WORKFLOW_METHOD_COMMANDED
  is CancelWorkflow -> WORKFLOW_CANCELED
  is CompleteTimers -> null
  is CompleteWorkflow -> null
  is RetryTasks -> WORKFLOW_TASKS_RETRY_COMMANDED
  is RetryWorkflowTask -> WORKFLOW_EXECUTOR_RETRY_COMMANDED
  is SendSignal -> WORKFLOW_SIGNAL_COMMANDED
  is WaitWorkflow -> null
}

fun WorkflowCmdMessage.toWorkflowJson(): JsonObject = when (this) {
  is DispatchWorkflow -> JsonObject(
      mapOf(
          WORKFLOW_META to workflowMeta.toJson(),
          WORKFLOW_TAGS to workflowTags.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is DispatchMethod -> JsonObject(
      mapOf(
          WORKFLOW_METHOD_ARGS to methodParameters.toJson(),
          WORKFLOW_METHOD_NAME to methodName.toJson(),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is CancelWorkflow -> JsonObject(
      mapOf(
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is CompleteTimers -> TODO()

  is CompleteWorkflow -> TODO()

  is RetryTasks -> JsonObject(
      mapOf(
          TARGET to JsonObject(
              mapOf(
                  TASK_ID to taskId.toJson(),
                  TASK_STATUS to taskStatus.toJson(),
                  SERVICE_NAME to serviceName.toJson(),
              ),
          ),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is RetryWorkflowTask -> JsonObject(
      mapOf(
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is SendSignal -> JsonObject(
      mapOf(
          CHANNEL_NAME to channelName.toJson(),
          SIGNAL_ID to signalId.toJson(),
          SIGNAL_DATA to signalData.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is WaitWorkflow -> thisShouldNotHappen()
}
