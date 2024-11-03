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
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.workflows.data.commands.DispatchNewMethodCommand
import io.infinitic.common.workflows.data.commands.DispatchNewMethodPastCommand
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.engine.commands.dispatchRemoteMethod
import io.infinitic.common.workflows.engine.messages.RemoteMethodDispatchedEvent
import io.infinitic.common.workflows.engine.messages.data.RemoteMethodDispatchedById
import io.infinitic.common.workflows.engine.messages.data.RemoteMethodDispatchedByTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.dispatchRemoteMethodCmd(
  current: WorkflowRequester,
  pastCommand: DispatchNewMethodPastCommand,
  workflowTaskInstant: MillisInstant,
  producer: InfiniticProducer,
) = launch {
  val emitterName = EmitterName(producer.getName())
  val dispatchNewMethod: DispatchNewMethodCommand = pastCommand.command
  val workflowMethodId = WorkflowMethodId.from(pastCommand.commandId)

  // Description of the dispatched remote method
  val remoteMethod = when {
    dispatchNewMethod.workflowId != null -> with(dispatchNewMethod) {
      RemoteMethodDispatchedById(
          workflowName = workflowName,
          workflowId = workflowId!!,
          workflowMethodName = methodName,
          workflowMethodId = workflowMethodId,
          methodName = methodName,
          methodParameters = methodParameters,
          methodParameterTypes = methodParameterTypes,
          timeout = methodTimeout,
          emittedAt = workflowTaskInstant,
      )
    }

    dispatchNewMethod.workflowTag != null -> with(dispatchNewMethod) {
      RemoteMethodDispatchedByTag(
          workflowName = workflowName,
          workflowTag = workflowTag!!,
          workflowMethodName = methodName,
          workflowMethodId = workflowMethodId,
          methodName = methodName,
          methodParameters = methodParameters,
          methodParameterTypes = methodParameterTypes,
          timeout = methodTimeout,
          emittedAt = workflowTaskInstant,
      )
    }

    else -> thisShouldNotHappen()
  }
  // Dispatching of the remote method
  with(producer) { dispatchRemoteMethod(remoteMethod, current) }

  // Description of the workflow event
  val remoteMethodDispatchedEvent = RemoteMethodDispatchedEvent(
      remoteMethodDispatched = remoteMethod,
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
