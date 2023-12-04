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
import io.infinitic.common.tasks.executors.errors.MethodTimedOutError
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.data.commands.DispatchExistingWorkflowCommand
import io.infinitic.common.workflows.data.commands.DispatchExistingWorkflowPastCommand
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.engine.messages.ChildMethodTimedOut
import io.infinitic.common.workflows.engine.messages.DispatchMethodOnRunningWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.tags.messages.DispatchMethodByTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.dispatchExistingWorkflowCmd(
  pastCommand: DispatchExistingWorkflowPastCommand,
  state: WorkflowState,
  producer: InfiniticProducer,
  bufferedMessages: MutableList<WorkflowEngineMessage>
) {
  val command: DispatchExistingWorkflowCommand = pastCommand.command
  val methodRunId = MethodRunId.from(pastCommand.commandId)
  val timeout = command.methodTimeout
  val clientName = ClientName(producer.name)

  when {
    command.workflowId != null -> {
      val dispatchMethodRun = getDispatchMethod(clientName, methodRunId, command, state)

      when (command.workflowId) {
        // dispatch method on this workflow
        state.workflowId -> bufferedMessages.add(dispatchMethodRun)

        // dispatch method on another workflow
        else -> launch { producer.send(dispatchMethodRun) }
      }

      // set timeout if any
      if (timeout != null) {
        val childMethodTimedOut = ChildMethodTimedOut(
            workflowName = state.workflowName,
            workflowId = state.workflowId,
            methodRunId = state.runningMethodRunId ?: thisShouldNotHappen(),
            childMethodTimedOutError = MethodTimedOutError(
                workflowName = command.workflowName,
                workflowId = command.workflowId!!,
                methodName = command.methodName,
                methodRunId = methodRunId,
            ),
            emitterName = clientName,
        )

        launch { producer.send(childMethodTimedOut, timeout) }
      }
    }

    command.workflowTag != null -> {
      if (state.workflowTags.contains(command.workflowTag!!)) {
        // dispatch method on this workflow
        bufferedMessages.add(
            getDispatchMethod(ClientName(producer.name), methodRunId, command, state),
        )
      }

      val dispatchMethodByTag = DispatchMethodByTag(
          workflowName = command.workflowName,
          workflowTag = command.workflowTag!!,
          parentWorkflowId = state.workflowId,
          parentWorkflowName = state.workflowName,
          parentMethodRunId = state.runningMethodRunId,
          methodRunId = methodRunId,
          methodName = command.methodName,
          methodParameterTypes = command.methodParameterTypes,
          methodParameters = command.methodParameters,
          methodTimeout = timeout,
          clientWaiting = false,
          emitterName = clientName,
      )
      // tag engine must ignore this message if parentWorkflowId has the provided tag
      launch { producer.send(dispatchMethodByTag) }
    }

    else -> thisShouldNotHappen()
  }
}

private fun getDispatchMethod(
  emitterName: ClientName,
  methodRunId: MethodRunId,
  command: DispatchExistingWorkflowCommand,
  state: WorkflowState
) = DispatchMethodOnRunningWorkflow(
    workflowName = command.workflowName,
    workflowId = command.workflowId!!,
    methodRunId = methodRunId,
    methodName = command.methodName,
    methodParameters = command.methodParameters,
    methodParameterTypes = command.methodParameterTypes,
    parentWorkflowId = state.workflowId,
    parentWorkflowName = state.workflowName,
    parentMethodRunId = state.runningMethodRunId,
    clientWaiting = false,
    emitterName = emitterName,
)
