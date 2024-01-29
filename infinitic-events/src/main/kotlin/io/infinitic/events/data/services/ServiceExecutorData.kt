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

package io.infinitic.events.data.services

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.executors.messages.clientName
import io.infinitic.events.InfiniticEventType
import io.infinitic.events.data.ClientRequesterData
import io.infinitic.events.data.RequesterData
import io.infinitic.events.data.WorkflowRequesterData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

fun ServiceExecutorMessage.serviceType(): InfiniticEventType = when (this) {
  is ExecuteTask -> InfiniticEventType.TASK_DISPATCHED
}

@Serializable
data class TaskDispatchedData(
  val taskName: String,
  val taskArgs: List<JsonElement>,
  val requester: RequesterData,
  override val retrySequence: Int,
  override val retryIndex: Int,
  override val taskMeta: Map<String, ByteArray>,
  override val taskTags: Set<String>,
  override val infiniticVersion: String
) : ServiceEventData

fun ServiceExecutorMessage.toServiceData() = when (this) {
  is ExecuteTask -> TaskDispatchedData(
      retrySequence = taskRetrySequence.toInt(),
      retryIndex = taskRetryIndex.toInt(),
      taskName = methodName.toString(),
      taskArgs = methodParameters.toJson(),
      taskMeta = taskMeta.map,
      taskTags = taskTags.map { it.toString() }.toSet(),
      requester = when {
        workflowName != null -> WorkflowRequesterData(
            workflowName = workflowName.toString(),
            workflowId = workflowId?.toString() ?: thisShouldNotHappen(),
            workflowMethodId = workflowMethodId?.toString() ?: thisShouldNotHappen(),
        )

        clientName != null -> ClientRequesterData(
            clientName = clientName.toString(),
        )

        else -> thisShouldNotHappen()
      },
      infiniticVersion = version.toString(),
  )
}
