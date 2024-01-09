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
package io.infinitic.tasks.executor.commands

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.commands.SendSignalCommand
import io.infinitic.common.workflows.data.commands.SendSignalPastCommand
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.tasks.executor.TaskEventHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.sendSignalCmd(
  currentWorkflow: TaskEventHandler.CurrentWorkflow,
  pastCommand: SendSignalPastCommand,
  workflowTaskInstant: MillisInstant,
  producer: InfiniticProducer,
) {
  val emitterName = EmitterName(producer.name)
  val command: SendSignalCommand = pastCommand.command

  when {
    command.workflowId != null -> {
      if (command.workflowId != currentWorkflow.workflowId) {
        launch {
          val sendToChannel = SendSignal(
              channelName = command.channelName,
              signalId = SignalId.from(pastCommand.commandId),
              signalData = command.signalData,
              channelTypes = command.channelTypes,
              workflowName = command.workflowName,
              workflowId = command.workflowId!!,
              emitterName = emitterName,
              emittedAt = workflowTaskInstant,
          )

          // dispatch signal on another workflow
          producer.sendToWorkflowCmd(sendToChannel)
        }
      }
    }

    command.workflowTag != null -> {
      launch {
        // dispatch signal per tag
        val sendSignalByTag = SendSignalByTag(
            workflowName = command.workflowName,
            workflowTag = command.workflowTag!!,
            channelName = command.channelName,
            signalId = SignalId(),
            signalData = command.signalData,
            channelTypes = command.channelTypes,
            parentWorkflowId = currentWorkflow.workflowId,
            emitterName = emitterName,
            emittedAt = workflowTaskInstant,
        )
        // Note: tag engine MUST ignore this message for Id = parentWorkflowId
        producer.sendToWorkflowTag(sendSignalByTag)
      }
    }
  }
}

