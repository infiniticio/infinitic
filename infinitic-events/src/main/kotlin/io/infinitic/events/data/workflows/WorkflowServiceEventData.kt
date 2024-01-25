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
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.events.messages.TaskFailedEvent
import io.infinitic.events.InfiniticWorkflowTaskEventType
import io.infinitic.events.WorkflowTaskCompletedType
import io.infinitic.events.WorkflowTaskFailedType
import io.infinitic.events.data.ErrorData
import io.infinitic.events.data.toErrorData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

fun ServiceEventMessage.workflowType(): InfiniticWorkflowTaskEventType? =
    when (this.isWorkflowTask()) {
      false -> null
      true -> when (this) {
        is TaskCompletedEvent -> WorkflowTaskCompletedType
        is TaskFailedEvent -> WorkflowTaskFailedType
        else -> null
      }
    }

fun ServiceEventMessage.toWorkflowData(): WorkflowEventData = when (this.isWorkflowTask()) {
  false -> thisShouldNotHappen()
  true -> when (this) {
    is TaskCompletedEvent -> WorkflowTaskCompletedData(
        result = returnValue.toJson(),
        workerName = emitterName.toString(),
        infiniticVersion = version.toString(),
    )

    is TaskFailedEvent -> WorkflowTaskFailedData(
        deferredError = deferredError?.toDeferredErrorData(),
        error = executionError.toErrorData(),
        workerName = emitterName.toString(),
        infiniticVersion = version.toString(),
    )

    else -> thisShouldNotHappen()
  }
}

@Serializable
data class WorkflowTaskCompletedData(
  val result: JsonElement,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class WorkflowTaskFailedData(
  val deferredError: DeferredErrorData?,
  val error: ErrorData,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData
