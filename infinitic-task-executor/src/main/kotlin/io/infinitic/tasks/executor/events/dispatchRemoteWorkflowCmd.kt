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
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.workflows.data.commands.DispatchNewWorkflowPastCommand
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.customId
import io.infinitic.common.workflows.engine.commands.dispatchRemoteMethod
import io.infinitic.common.workflows.engine.messages.RemoteMethodDispatchedEvent
import io.infinitic.common.workflows.engine.messages.data.RemoteWorkflowDispatched
import io.infinitic.common.workflows.engine.messages.data.RemoteWorkflowDispatchedByCustomId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.dispatchRemoteWorkflowCmd(
  current: WorkflowRequester,
  pastCommand: DispatchNewWorkflowPastCommand,
  workflowTaskInstant: MillisInstant,
  producer: InfiniticProducer
) = launch {
  val emitterName = EmitterName(producer.getName())
  val dispatchNewWorkflow = pastCommand.command
  val customId = dispatchNewWorkflow.workflowTags.customId

  // Description of the dispatched remote workflow
  val remoteWorkflowDispatched = with(dispatchNewWorkflow) {
    when (customId) {
      null -> RemoteWorkflowDispatched(
          workflowId = WorkflowId.from(pastCommand.commandId),
          workflowName = workflowName,
          workflowMethodId = WorkflowMethodId.from(pastCommand.commandId),
          workflowMethodName = methodName,
          methodName = methodName,
          methodParameters = methodParameters,
          methodParameterTypes = methodParameterTypes,
          workflowTags = workflowTags,
          workflowMeta = workflowMeta,
          timeout = methodTimeout,
          emittedAt = workflowTaskInstant,
      )

      else -> RemoteWorkflowDispatchedByCustomId(
          customId = customId,
          workflowId = WorkflowId.from(pastCommand.commandId),
          workflowName = workflowName,
          workflowMethodId = WorkflowMethodId.from(pastCommand.commandId),
          workflowMethodName = methodName,
          methodName = methodName,
          methodParameters = methodParameters,
          methodParameterTypes = methodParameterTypes,
          workflowTags = workflowTags,
          workflowMeta = workflowMeta,
          timeout = methodTimeout,
          emittedAt = workflowTaskInstant,
      )
    }
  }
  // Dispatching the remote workflow
  with(producer) { dispatchRemoteMethod(remoteWorkflowDispatched, current) }

  // Description of the workflow event
  val remoteMethodDispatchedEvent = RemoteMethodDispatchedEvent(
      remoteMethodDispatched = remoteWorkflowDispatched,
      workflowName = current.workflowName,
      workflowId = current.workflowId,
      workflowVersion = current.workflowVersion,
      workflowMethodName = current.workflowMethodName,
      workflowMethodId = current.workflowMethodId,
      emitterName = emitterName,
  )
  // Dispatching the workflow event
  with(producer) { remoteMethodDispatchedEvent.sendTo(WorkflowStateEventTopic) }
}
