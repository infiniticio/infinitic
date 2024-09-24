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
package io.infinitic.common.workflows.engine.commands

import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.requester.Requester
import io.infinitic.common.requester.workflowId
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.data.RemoteSignalDispatched
import io.infinitic.common.workflows.engine.messages.data.RemoteSignalDispatchedById
import io.infinitic.common.workflows.engine.messages.data.RemoteSignalDispatchedByTag
import io.infinitic.common.workflows.tags.messages.SendSignalByTag

suspend fun InfiniticProducer.dispatchRemoteSignal(
  signal: RemoteSignalDispatched,
  requester: Requester
) {
  suspend fun getEmitterName() = EmitterName(getName())

  when (signal) {
    is RemoteSignalDispatchedById -> when (signal.workflowId) {
      requester.workflowId -> {
        // do nothing as the signal is handled from the workflowTaskCompleted
      }

      else -> {
        // dispatch signal to the other workflow
        val sendToChannel = with(signal) {
          SendSignal(
              workflowName = workflowName,
              workflowId = workflowId,
              channelName = channelName,
              signalId = signalId,
              signalData = signalData,
              channelTypes = channelTypes,
              emitterName = getEmitterName(),
              emittedAt = emittedAt,
              requester = requester,
          )
        }
        sendToChannel.sendTo(WorkflowStateCmdTopic)
      }
    }

    is RemoteSignalDispatchedByTag -> {
      // dispatch signal per tag
      val sendSignalByTag = with(signal) {
        SendSignalByTag(
            workflowName = workflowName,
            workflowTag = workflowTag,
            channelName = channelName,
            signalId = signalId,
            signalData = signalData,
            channelTypes = channelTypes,
            emitterName = getEmitterName(),
            emittedAt = emittedAt,
            requester = requester,
        )
      }
      // Note: tag engine MUST ignore this message for workflowId = requester.workflowId
      // as the signal is handled from the workflowTaskCompleted
      sendSignalByTag.sendTo(WorkflowTagEngineTopic)
    }
  }
}
