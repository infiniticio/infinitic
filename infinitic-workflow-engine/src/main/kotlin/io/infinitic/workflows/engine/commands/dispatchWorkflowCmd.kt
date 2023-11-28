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
import io.infinitic.common.workflows.data.commands.DispatchWorkflowCommand
import io.infinitic.common.workflows.data.commands.DispatchWorkflowPastCommand
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.tags.messages.AddTagToWorkflow
import io.infinitic.common.workflows.tags.messages.DispatchWorkflowByCustomId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.dispatchWorkflowCmd(
  newCommand: DispatchWorkflowPastCommand,
  state: WorkflowState,
  producer: InfiniticProducer
) {
  val command: DispatchWorkflowCommand = newCommand.command

  val customIds = command.workflowTags.filter { it.isCustomId() }

  when (customIds.size) {
    // no customId tag provided
    0 -> {
      // send workflow to workflow engine
      val dispatchWorkflow =
          DispatchWorkflow(
              workflowName = command.workflowName,
              workflowId = WorkflowId.from(newCommand.commandId),
              methodName = command.methodName,
              methodParameters = command.methodParameters,
              methodParameterTypes = command.methodParameterTypes,
              workflowTags = command.workflowTags,
              workflowMeta = command.workflowMeta,
              parentWorkflowName = state.workflowName,
              parentWorkflowId = state.workflowId,
              parentMethodRunId = state.runningMethodRunId,
              clientWaiting = false,
              emitterName = ClientName(producer.name),
          )
      launch { producer.send(dispatchWorkflow) }

      // add provided tags
      dispatchWorkflow.workflowTags.forEach {
        val addTagToWorkflow =
            AddTagToWorkflow(
                workflowName = dispatchWorkflow.workflowName,
                workflowTag = it,
                workflowId = dispatchWorkflow.workflowId,
                emitterName = ClientName(producer.name),
            )
        launch { producer.send(addTagToWorkflow) }
      }
    }

    1 -> {
      // dispatch workflow message with customId tag
      val dispatchWorkflowByCustomId =
          DispatchWorkflowByCustomId(
              workflowName = command.workflowName,
              workflowTag = customIds.first(),
              workflowId = WorkflowId.from(newCommand.commandId),
              methodName = command.methodName,
              methodParameters = command.methodParameters,
              methodParameterTypes = command.methodParameterTypes,
              workflowTags = command.workflowTags,
              workflowMeta = command.workflowMeta,
              parentWorkflowName = state.workflowName,
              parentWorkflowId = state.workflowId,
              parentMethodRunId = state.runningMethodRunId,
              clientWaiting = false,
              emitterName = ClientName(producer.name),
          )

      launch { producer.send(dispatchWorkflowByCustomId) }
    }
    // this must be excluded from workflow task
    else -> thisShouldNotHappen()
  }
}
