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

package io.infinitic.events.errors

import io.infinitic.cloudEvents.FAILURE
import io.infinitic.cloudEvents.METHOD_ID
import io.infinitic.cloudEvents.METHOD_NAME
import io.infinitic.cloudEvents.SERVICE_NAME
import io.infinitic.cloudEvents.TASK_ID
import io.infinitic.cloudEvents.TASK_NAME
import io.infinitic.cloudEvents.WORKFLOW_ID
import io.infinitic.cloudEvents.WORKFLOW_NAME
import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.tasks.executors.errors.MethodCanceledError
import io.infinitic.common.tasks.executors.errors.MethodFailedError
import io.infinitic.common.tasks.executors.errors.MethodTimedOutError
import io.infinitic.common.tasks.executors.errors.MethodUnknownError
import io.infinitic.common.tasks.executors.errors.TaskCanceledError
import io.infinitic.common.tasks.executors.errors.TaskFailedError
import io.infinitic.common.tasks.executors.errors.TaskTimedOutError
import io.infinitic.common.tasks.executors.errors.TaskUnknownError
import io.infinitic.common.tasks.executors.errors.WorkflowExecutorError
import io.infinitic.common.utils.toJson
import io.infinitic.events.types.EXECUTOR_FAILED
import io.infinitic.events.types.REMOTE_METHOD_CANCELED
import io.infinitic.events.types.REMOTE_METHOD_FAILED
import io.infinitic.events.types.REMOTE_METHOD_TIMED_OUT
import io.infinitic.events.types.REMOTE_METHOD_UNKNOWN
import io.infinitic.events.types.TASK_CANCELED
import io.infinitic.events.types.TASK_FAILED
import io.infinitic.events.types.TASK_TIMED_OUT
import io.infinitic.events.types.TASK_UNKNOWN
import kotlinx.serialization.json.JsonObject


fun DeferredError.toJson(): Pair<String, JsonObject> = when (this) {
  is WorkflowExecutorError -> EXECUTOR_FAILED to JsonObject(
      mapOf(
          FAILURE to lastFailure.toJson(),
          TASK_ID to workflowTaskId.toJson(),
      ),
  )

  is TaskFailedError -> TASK_FAILED to JsonObject(
      mapOf(
          FAILURE to lastFailure.toJson(),
          TASK_ID to taskId.toJson(),
          TASK_NAME to methodName.toJson(),
          SERVICE_NAME to serviceName.toJson(),
      ),
  )

  is TaskCanceledError -> TASK_CANCELED to JsonObject(
      mapOf(
          TASK_ID to taskId.toJson(),
          TASK_NAME to methodName.toJson(),
          SERVICE_NAME to serviceName.toJson(),
      ),
  )

  is TaskTimedOutError -> TASK_TIMED_OUT to JsonObject(
      mapOf(
          TASK_ID to taskId.toJson(),
          TASK_NAME to methodName.toJson(),
          SERVICE_NAME to serviceName.toJson(),
      ),
  )

  is TaskUnknownError -> TASK_UNKNOWN to JsonObject(
      mapOf(
          TASK_ID to taskId.toJson(),
          TASK_NAME to methodName.toJson(),
          SERVICE_NAME to serviceName.toJson(),
      ),
  )

  is MethodFailedError -> REMOTE_METHOD_FAILED to JsonObject(
      mapOf(
          deferredError.toJson(),
          WORKFLOW_ID to workflowId.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
      ),
  )

  is MethodCanceledError -> REMOTE_METHOD_CANCELED to JsonObject(
      mapOf(
          WORKFLOW_ID to workflowId.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
      ),
  )

  is MethodTimedOutError -> REMOTE_METHOD_TIMED_OUT to JsonObject(
      mapOf(
          WORKFLOW_ID to workflowId.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
      ),
  )

  is MethodUnknownError -> REMOTE_METHOD_UNKNOWN to JsonObject(
      mapOf(
          WORKFLOW_ID to workflowId.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_NAME to workflowMethodName.toJson(),
      ),
  )
}

