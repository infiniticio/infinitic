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
package io.infinitic.workflows.engine.commands

import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.SendSignalCommand
import io.infinitic.common.workflows.data.commands.SendSignalPastCommand
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.sendSignalCmd(
  pastCommand: SendSignalPastCommand,
  state: WorkflowState,
  producer: InfiniticProducer,
  bufferedMessages: MutableList<WorkflowEngineMessage>
) {
  val emitterName = EmitterName(producer.name)
  val command: SendSignalCommand = pastCommand.command

  when {
    command.workflowId != null -> {
      val sendToChannel = getSendSignal(emitterName, pastCommand.commandId, command)

      when (command.workflowId) {
        state.workflowId ->
          // dispatch signal on current workflow
          bufferedMessages.add(sendToChannel)

        else ->
          // dispatch signal on another workflow
          launch { producer.sendToWorkflowEngine(sendToChannel) }
      }
    }

    command.workflowTag != null -> {
      if (state.workflowTags.contains(command.workflowTag!!)) {
        val sendToChannel = getSendSignal(emitterName, pastCommand.commandId, command)
        bufferedMessages.add(sendToChannel)
      }
      // dispatch signal per tag
      val sendSignalByTag =
          SendSignalByTag(
              workflowName = command.workflowName,
              workflowTag = command.workflowTag!!,
              channelName = command.channelName,
              signalId = SignalId(),
              signalData = command.signalData,
              channelTypes = command.channelTypes,
              emitterWorkflowId = state.workflowId,
              emitterName = emitterName,
          )
      launch { producer.sendToWorkflowTag(sendSignalByTag) }
    }

    else -> thisShouldNotHappen()
  }
}

private fun getSendSignal(
  emitterName: EmitterName,
  commandId: CommandId,
  command: SendSignalCommand
) =
    SendSignal(
        channelName = command.channelName,
        signalId = SignalId.from(commandId),
        signalData = command.signalData,
        channelTypes = command.channelTypes,
        workflowName = command.workflowName,
        workflowId = command.workflowId!!,
        emitterName = emitterName,
    )
