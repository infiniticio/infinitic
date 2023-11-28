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

import io.infinitic.common.data.ClientName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.DispatchMethodCommand
import io.infinitic.common.workflows.data.commands.DispatchMethodPastCommand
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.tags.messages.DispatchMethodByTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.dispatchMethodCmd(
  newCommand: DispatchMethodPastCommand,
  state: WorkflowState,
  producer: InfiniticProducer,
  bufferedMessages: MutableList<WorkflowEngineMessage>
) {
  val command: DispatchMethodCommand = newCommand.command

  when {
    command.workflowId != null -> {
      val dispatchMethodRun =
          getDispatchMethod(ClientName(producer.name), newCommand.commandId, command, state)

      when (command.workflowId) {
        state.workflowId ->
          // dispatch method on this workflow
          bufferedMessages.add(dispatchMethodRun)

        else ->
          // dispatch method on another workflow
          launch { producer.send(dispatchMethodRun) }
      }
    }

    command.workflowTag != null -> {
      if (state.workflowTags.contains(command.workflowTag!!)) {
        // dispatch method on this workflow
        bufferedMessages.add(
            getDispatchMethod(ClientName(producer.name), newCommand.commandId, command, state),
        )
      }

      val dispatchMethodByTag =
          DispatchMethodByTag(
              workflowName = command.workflowName,
              workflowTag = command.workflowTag!!,
              parentWorkflowId = state.workflowId,
              parentWorkflowName = state.workflowName,
              parentMethodRunId = state.runningMethodRunId,
              methodRunId = MethodRunId.from(newCommand.commandId),
              methodName = command.methodName,
              methodParameterTypes = command.methodParameterTypes,
              methodParameters = command.methodParameters,
              clientWaiting = false,
              emitterName = ClientName(producer.name),
          )
      // tag engine must ignore this message if parentWorkflowId has the provided tag
      launch { producer.send(dispatchMethodByTag) }
    }

    else -> thisShouldNotHappen()
  }
}

private fun getDispatchMethod(
  emitterName: ClientName,
  commandId: CommandId,
  command: DispatchMethodCommand,
  state: WorkflowState
) =
    DispatchMethod(
        workflowName = command.workflowName,
        workflowId = command.workflowId!!,
        methodRunId = MethodRunId.from(commandId),
        methodName = command.methodName,
        methodParameters = command.methodParameters,
        methodParameterTypes = command.methodParameterTypes,
        parentWorkflowId = state.workflowId,
        parentWorkflowName = state.workflowName,
        parentMethodRunId = state.runningMethodRunId,
        clientWaiting = false,
        emitterName = emitterName,
    )
