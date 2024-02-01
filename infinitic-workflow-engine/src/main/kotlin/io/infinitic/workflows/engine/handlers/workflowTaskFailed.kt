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
import io.infinitic.common.workflows.data.workflowMethods.awaitingRequesters
import io.infinitic.common.workflows.engine.messages.MethodFailedEvent
import io.infinitic.common.workflows.engine.messages.RemoteTaskFailed
import io.infinitic.common.workflows.engine.state.WorkflowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.workflowTaskFailed(
  producer: InfiniticProducer,
  state: WorkflowState,
  message: RemoteTaskFailed
) {
  val emitterName = EmitterName(producer.name)
  val emittedAt = state.runningWorkflowTaskInstant ?: thisShouldNotHappen()

  val workflowMethod = state.getRunningWorkflowMethod()

  val deferredError = when (val error = message.deferredError) {
    null -> message.taskFailedError
    else -> error
  }

  val methodFailedEvent = MethodFailedEvent(
      workflowName = state.workflowName,
      workflowId = state.workflowId,
      workflowMethodId = workflowMethod.workflowMethodId,
      workflowMethodName = workflowMethod.methodName,
      awaitingRequesters = workflowMethod.awaitingRequesters,
      emitterName = emitterName,
      deferredError = deferredError,
  )
  launch { with(producer) { methodFailedEvent.sendTo(WorkflowEventsTopic) } }

  // send info to itself by adding a fake message on messageBuffer
  methodFailedEvent.getEventForAwaitingWorkflows(emitterName, emittedAt)
      .firstOrNull { it.workflowId == message.workflowId }
      ?.let { state.messagesBuffer.add(0, it) }
}
