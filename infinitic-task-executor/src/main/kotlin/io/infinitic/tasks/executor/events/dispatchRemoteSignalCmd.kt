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
package io.infinitic.tasks.executor.events

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.commands.SendSignalCommand
import io.infinitic.common.workflows.data.commands.SendSignalPastCommand
import io.infinitic.common.workflows.engine.commands.dispatchRemoteSignal
import io.infinitic.common.workflows.engine.messages.SignalDispatchedEvent
import io.infinitic.common.workflows.engine.messages.data.RemoteSignalDispatchedById
import io.infinitic.common.workflows.engine.messages.data.RemoteSignalDispatchedByTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.dispatchRemoteSignalCmd(
  current: WorkflowRequester,
  pastCommand: SendSignalPastCommand,
  workflowTaskInstant: MillisInstant,
  producer: InfiniticProducer,
) = launch {
  val emitterName = EmitterName(producer.name)
  val command: SendSignalCommand = pastCommand.command
  val signalId = SignalId.from(pastCommand.commandId)
  val signalData = command.signalData

  // Description of the dispatched remote signal
  val remoteSignal = when {
    command.workflowId != null -> RemoteSignalDispatchedById(
        workflowName = command.workflowName,
        workflowId = command.workflowId!!,
        signalId = signalId,
        signalData = signalData,
        channelName = command.channelName,
        channelTypes = command.channelTypes,
        emittedAt = workflowTaskInstant,
    )

    command.workflowTag != null -> RemoteSignalDispatchedByTag(
        workflowName = command.workflowName,
        workflowTag = command.workflowTag!!,
        signalId = signalId,
        signalData = command.signalData,
        channelName = command.channelName,
        channelTypes = command.channelTypes,
        emittedAt = workflowTaskInstant,
    )

    else -> thisShouldNotHappen()
  }
  // Dispatching of the remote signal
  with(producer) { dispatchRemoteSignal(remoteSignal, current) }

  // Description of the workflow event
  val signalDispatchedEvent = SignalDispatchedEvent(
      remoteSignalDispatched = remoteSignal,
      workflowName = current.workflowName,
      workflowId = current.workflowId,
      workflowVersion = current.workflowVersion,
      workflowMethodName = current.workflowMethodName,
      workflowMethodId = current.workflowMethodId,
      emitterName = emitterName,
  )
  // Dispatching the workflow event
  with(producer) { signalDispatchedEvent.sendTo(WorkflowEventsTopic) }
}


