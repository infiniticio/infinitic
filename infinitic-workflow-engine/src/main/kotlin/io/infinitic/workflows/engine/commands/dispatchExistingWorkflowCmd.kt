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
import io.infinitic.common.tasks.executors.errors.WorkflowMethodTimedOutError
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.data.commands.DispatchExistingWorkflowPastCommand
import io.infinitic.common.workflows.data.commands.DispatchMethodOnRunningWorkflowCommand
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethodId
import io.infinitic.common.workflows.engine.messages.ChildMethodTimedOut
import io.infinitic.common.workflows.engine.messages.DispatchMethodWorkflow
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
  val emitterName = EmitterName(producer.name)
  val command: DispatchMethodOnRunningWorkflowCommand = pastCommand.command
  val methodRunId = WorkflowMethodId.from(pastCommand.commandId)
  val timeout = command.methodTimeout

  when {
    command.workflowId != null -> {
      val dispatchMethodRun = getDispatchMethod(emitterName, methodRunId, command, state)

      when (command.workflowId) {
        // dispatch method on this workflow
        state.workflowId -> bufferedMessages.add(dispatchMethodRun)

        // dispatch method on another workflow
        else -> launch { producer.sendToWorkflowEngine(dispatchMethodRun) }
      }

      // set timeout if any
      if (timeout != null) {
        val childMethodTimedOut = ChildMethodTimedOut(
            childMethodTimedOutError = WorkflowMethodTimedOutError(
                workflowName = command.workflowName,
                workflowId = command.workflowId!!,
                methodName = command.methodName,
                workflowMethodId = methodRunId,
            ),
            workflowName = state.workflowName,
            workflowId = state.workflowId,
            workflowMethodId = state.runningWorkflowMethodId ?: thisShouldNotHappen(),
            emitterName = emitterName,
        )

        launch { producer.sendToWorkflowEngine(childMethodTimedOut, timeout) }
      }
    }

    command.workflowTag != null -> {
      if (state.workflowTags.contains(command.workflowTag!!)) {
        // dispatch method on this workflow
        bufferedMessages.add(
            getDispatchMethod(emitterName, methodRunId, command, state),
        )
      }

      val dispatchMethodByTag = DispatchMethodByTag(
          workflowName = command.workflowName,
          workflowTag = command.workflowTag!!,
          parentWorkflowId = state.workflowId,
          parentWorkflowName = state.workflowName,
          parentWorkflowMethodId = state.runningWorkflowMethodId,
          workflowMethodId = methodRunId,
          methodName = command.methodName,
          methodParameterTypes = command.methodParameterTypes,
          methodParameters = command.methodParameters,
          methodTimeout = timeout,
          clientWaiting = false,
          emitterName = emitterName,
      )
      // tag engine must ignore this message if parentWorkflowId has the provided tag
      launch { producer.sendToWorkflowTag(dispatchMethodByTag) }
    }

    else -> thisShouldNotHappen()
  }
}

private fun getDispatchMethod(
  emitterName: EmitterName,
  workflowMethodId: WorkflowMethodId,
  command: DispatchMethodOnRunningWorkflowCommand,
  state: WorkflowState
) = DispatchMethodWorkflow(
    workflowName = command.workflowName,
    workflowId = command.workflowId!!,
    workflowMethodId = workflowMethodId,
    methodName = command.methodName,
    methodParameters = command.methodParameters,
    methodParameterTypes = command.methodParameterTypes,
    parentWorkflowId = state.workflowId,
    parentWorkflowName = state.workflowName,
    parentWorkflowMethodId = state.runningWorkflowMethodId,
    clientWaiting = false,
    emitterName = emitterName,
)
