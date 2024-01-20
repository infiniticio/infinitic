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

package io.infinitic.events.data

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.executors.messages.clientName
import io.infinitic.events.InfiniticEventType

fun ServiceExecutorMessage.type() = when (this) {
  is ExecuteTask -> InfiniticEventType.TASK_REQUESTED
}

sealed interface ServiceExecutorData : MessageData

data class TaskRequestedData(
  val retrySequence: Int,
  val retryIndex: Int,
  val name: String,
  val args: List<String>,
  val meta: Map<String, ByteArray>,
  val tags: Set<String>,
  val requester: RequesterData
) : ServiceExecutorData

fun ServiceExecutorMessage.toData() = when (this) {
  is ExecuteTask -> TaskRequestedData(
      retrySequence = taskRetrySequence.toInt(),
      retryIndex = taskRetryIndex.toInt(),
      name = methodName.toString(),
      args = methodParameters.map { String(it.bytes) },
      meta = taskMeta.map,
      tags = taskTags.map { it.toString() }.toSet(),
      requester = when {
        workflowName != null -> WorkflowRequesterData(
            workflowName = workflowName.toString(),
            workflowId = workflowId?.toString() ?: thisShouldNotHappen(),
            methodId = workflowMethodId?.toString() ?: thisShouldNotHappen(),
            workerName = emitterName.toString(),
        )

        clientName != null -> ClientRequesterData(
            clientName = clientName.toString(),
        )

        else -> thisShouldNotHappen()
      },
  )
}
