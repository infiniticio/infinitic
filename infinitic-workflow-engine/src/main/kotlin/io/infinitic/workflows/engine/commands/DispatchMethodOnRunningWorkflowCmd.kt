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
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.data.commands.DispatchMethodOnRunningWorkflowCommand
import io.infinitic.common.workflows.data.commands.DispatchMethodOnRunningWorkflowPastCommand
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethodId
import io.infinitic.common.workflows.engine.messages.DispatchMethodWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.state.WorkflowState

internal fun dispatchMethodOnRunningWorkflowCmd(
  pastCommand: DispatchMethodOnRunningWorkflowPastCommand,
  state: WorkflowState,
  producer: InfiniticProducer,
  bufferedMessages: MutableList<WorkflowEngineMessage>
) {
  val command: DispatchMethodOnRunningWorkflowCommand = pastCommand.command

  if (
    (command.workflowId != null && state.workflowId == command.workflowId) ||
    (command.workflowTag != null && state.workflowTags.contains(command.workflowTag))
  ) {
    val dispatchMethodWorkflow = DispatchMethodWorkflow(
        workflowName = command.workflowName,
        workflowId = command.workflowId!!,
        workflowMethodId = WorkflowMethodId.from(pastCommand.commandId),
        methodName = command.methodName,
        methodParameters = command.methodParameters,
        methodParameterTypes = command.methodParameterTypes,
        parentWorkflowId = state.workflowId,
        parentWorkflowName = state.workflowName,
        parentWorkflowMethodId = state.runningWorkflowMethodId,
        clientWaiting = false,
        emitterName = EmitterName(producer.name),
    )
    bufferedMessages.add(dispatchMethodWorkflow)
  }
}

