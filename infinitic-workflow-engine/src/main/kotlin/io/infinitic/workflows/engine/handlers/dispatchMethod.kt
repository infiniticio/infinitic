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
package io.infinitic.workflows.engine.handlers

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.workflows.data.workflowMethods.PositionInWorkflowMethod
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethod
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.MethodStartedEvent
import io.infinitic.common.workflows.engine.messages.parentClientName
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.workflows.engine.helpers.dispatchWorkflowTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * This method is called when a client manually dispatches a method on a running workflow
 */
internal fun CoroutineScope.dispatchMethod(
  producer: InfiniticProducer,
  state: WorkflowState,
  message: DispatchMethod
) {
  launch {
    val methodStartedEvent = MethodStartedEvent(
        workflowName = message.workflowName,
        workflowId = message.workflowId,
        emitterName = EmitterName(producer.name),
        workflowMethodId = WorkflowMethodId.from(message.workflowId),
    )
    with(producer) { methodStartedEvent.sendTo(WorkflowEventsTopic) }
  }

  val workflowMethod = WorkflowMethod(
      workflowMethodId = message.workflowMethodId,
      waitingClients =
      when (message.clientWaiting) {
        true -> mutableSetOf(ClientName.from(message.emitterName))
        false -> mutableSetOf()
      },
      parentWorkflowId = message.requesterWorkflowId,
      parentWorkflowName = message.requesterWorkflowName,
      parentWorkflowMethodId = message.requesterWorkflowMethodId,
      parentClientName = message.parentClientName,
      methodName = message.methodName,
      methodParameterTypes = message.methodParameterTypes,
      methodParameters = message.methodParameters,
      workflowTaskIndexAtStart = state.workflowTaskIndex,
      propertiesNameHashAtStart = state.currentPropertiesNameHash.toMap(),
  )

  state.workflowMethods.add(workflowMethod)

  dispatchWorkflowTask(
      producer,
      state,
      workflowMethod,
      PositionInWorkflowMethod(),
      message.emittedAt ?: thisShouldNotHappen(),
  )
}
