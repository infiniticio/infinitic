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

import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.workflows.data.workflowMethods.PositionInWorkflowMethod
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethod
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.common.workflows.engine.messages.DispatchNewWorkflow
import io.infinitic.common.workflows.engine.messages.MethodStartedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowStartedEvent
import io.infinitic.common.workflows.engine.messages.parentClientName
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.workflows.engine.helpers.dispatchWorkflowTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Deprecated("This should be removed after v0.13.0")
internal fun CoroutineScope.dispatchWorkflow(
  producer: InfiniticProducer,
  message: DispatchNewWorkflow
): WorkflowState {

  val workflowMethod = WorkflowMethod(
      workflowMethodId = WorkflowMethodId.from(message.workflowId),
      waitingClients = message.waitingClients(),
      parentWorkflowId = message.requesterWorkflowId,
      parentWorkflowName = message.requesterWorkflowName,
      parentWorkflowMethodId = message.requesterWorkflowMethodId,
      parentClientName = message.parentClientName,
      methodName = message.methodName,
      methodParameterTypes = message.methodParameterTypes,
      methodParameters = message.methodParameters,
      workflowTaskIndexAtStart = WorkflowTaskIndex(0),
      propertiesNameHashAtStart = mapOf(),
  )

  val state = WorkflowState(
      lastMessageId = message.messageId,
      workflowId = message.workflowId,
      workflowName = message.workflowName,
      workflowVersion = null,
      workflowTags = message.workflowTags,
      workflowMeta = message.workflowMeta,
      workflowMethods = mutableListOf(workflowMethod),
  )

  dispatchWorkflowTask(
      producer,
      state,
      workflowMethod,
      PositionInWorkflowMethod(),
      message.emittedAt ?: thisShouldNotHappen(),
  )

  launch {
    val emitterName = EmitterName(producer.name)

    val workflowStartedEvent = WorkflowStartedEvent(
        workflowName = message.workflowName,
        workflowId = message.workflowId,
        emitterName = emitterName,
    )

    val methodStartedEvent = MethodStartedEvent(
        workflowName = state.workflowName,
        workflowId = state.workflowId,
        emitterName = emitterName,
        workflowMethodId = workflowMethod.workflowMethodId,
    )

    // the 2 events are sent sequentially, to ensure they have consistent timestamps
    // (workflowStarted before workflowMethodStarted)
    with(producer) {
      workflowStartedEvent.sendTo(WorkflowEventsTopic)
      methodStartedEvent.sendTo(WorkflowEventsTopic)
    }
  }

  return state
}
