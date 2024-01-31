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

import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.tasks.executors.errors.MethodCanceledError
import io.infinitic.common.tasks.executors.errors.MethodFailedError
import io.infinitic.common.tasks.executors.errors.MethodTimedOutError
import io.infinitic.common.tasks.executors.errors.MethodUnknownError
import io.infinitic.common.tasks.executors.errors.TaskCanceledError
import io.infinitic.common.tasks.executors.errors.TaskFailedError
import io.infinitic.common.tasks.executors.errors.TaskTimedOutError
import io.infinitic.common.tasks.executors.errors.TaskUnknownError
import io.infinitic.common.tasks.executors.errors.WorkflowTaskFailedError
import io.infinitic.common.utils.toJson
import io.infinitic.events.properties.CHILD_WORKFLOW_CANCELED
import io.infinitic.events.properties.CHILD_WORKFLOW_FAILED
import io.infinitic.events.properties.CHILD_WORKFLOW_TIMED_OUT
import io.infinitic.events.properties.CHILD_WORKFLOW_UNKNOWN
import io.infinitic.events.properties.ERROR
import io.infinitic.events.properties.SERVICE_NAME
import io.infinitic.events.properties.TASK_CANCELED
import io.infinitic.events.properties.TASK_FAILED
import io.infinitic.events.properties.TASK_ID
import io.infinitic.events.properties.TASK_NAME
import io.infinitic.events.properties.TASK_TIMED_OUT
import io.infinitic.events.properties.TASK_UNKNOWN
import io.infinitic.events.properties.WORKFLOW_EXECUTOR_FAILED
import io.infinitic.events.properties.WORKFLOW_ID
import io.infinitic.events.properties.WORKFLOW_METHOD_ID
import io.infinitic.events.properties.WORKFLOW_NAME
import kotlinx.serialization.json.JsonObject


fun DeferredError.toJson(): Pair<String, JsonObject> = when (this) {
  is WorkflowTaskFailedError -> WORKFLOW_EXECUTOR_FAILED to JsonObject(
      mapOf(
          ERROR to cause.toJson(),
          TASK_ID to workflowTaskId.toJson(),
      ),
  )

  is TaskFailedError -> TASK_FAILED to JsonObject(
      mapOf(
          ERROR to cause.toJson(),
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
          SERVICE_NAME to serviceName.toJson(),
      ),
  )

  is MethodFailedError -> CHILD_WORKFLOW_FAILED to JsonObject(
      mapOf(
          deferredError.toJson(),
          WORKFLOW_ID to workflowId.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
      ),
  )

  is MethodCanceledError -> CHILD_WORKFLOW_CANCELED to JsonObject(
      mapOf(
          WORKFLOW_ID to workflowId.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
      ),
  )

  is MethodTimedOutError -> CHILD_WORKFLOW_TIMED_OUT to JsonObject(
      mapOf(
          WORKFLOW_ID to workflowId.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
      ),
  )

  is MethodUnknownError -> CHILD_WORKFLOW_UNKNOWN to JsonObject(
      mapOf(
          WORKFLOW_ID to workflowId.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_METHOD_ID to workflowMethodId.toJson(),
      ),
  )
}

