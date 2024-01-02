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

import io.infinitic.common.clients.messages.MethodFailed
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.executors.errors.MethodFailedError
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.engine.events.WorkflowMethodFailedEvent
import io.infinitic.common.workflows.engine.messages.ChildMethodFailed
import io.infinitic.common.workflows.engine.messages.TaskFailed
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.state.WorkflowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.workflowTaskFailed(
  producer: InfiniticProducer,
  state: WorkflowState,
  message: TaskFailed
) {
  val emitterName = EmitterName(producer.name)
  val workflowMethod = state.getRunningWorkflowMethod()

  val deferredError =
      when (val deferredError = message.deferredError) {
        null -> message.taskFailedError
        else -> deferredError
      }

  launch {
    val workflowMethodFailedEvent = WorkflowMethodFailedEvent(
        workflowName = state.workflowName,
        workflowId = state.workflowId,
        workflowMethodId = workflowMethod.workflowMethodId,
        parentWorkflowId = workflowMethod.parentWorkflowId,
        parentWorkflowName = workflowMethod.parentWorkflowName,
        parentWorkflowMethodId = workflowMethod.parentWorkflowMethodId,
        parentClientName = workflowMethod.parentClientName,
        waitingClients = workflowMethod.waitingClients,
        workflowMeta = state.workflowMeta,
        workflowTags = state.workflowTags,
        emitterName = emitterName,
        deferredError = deferredError,
    )

    producer.sendToWorkflowEvents(workflowMethodFailedEvent)
  }

  // send to waiting clients
  workflowMethod.waitingClients.forEach {
    launch {
      val methodFailed = MethodFailed(
          recipientName = it,
          workflowId = state.workflowId,
          workflowMethodId = workflowMethod.workflowMethodId,
          cause = deferredError,
          emitterName = emitterName,
      )
      producer.sendToClient(methodFailed)
    }
  }
  workflowMethod.waitingClients.clear()

  val bufferedMessages = mutableListOf<WorkflowEngineMessage>()

  // send to parent workflow
  workflowMethod.parentWorkflowId?.let {
    val childMethodFailed = ChildMethodFailed(
        childMethodFailedError =
        MethodFailedError(
            workflowName = state.workflowName,
            workflowId = state.workflowId,
            workflowMethodName = workflowMethod.methodName,
            workflowMethodId = workflowMethod.workflowMethodId,
            deferredError = deferredError,
        ),
        workflowName = workflowMethod.parentWorkflowName ?: thisShouldNotHappen(),
        workflowId = it,
        workflowMethodId = workflowMethod.parentWorkflowMethodId ?: thisShouldNotHappen(),
        emitterName = emitterName,
    )
    if (it == state.workflowId) {
      // case of method dispatched within same workflow
      bufferedMessages.add(childMethodFailed)
    } else {
      launch { producer.sendToWorkflowEngine(childMethodFailed) }
    }
  }

  // add fake message at the top of the messagesBuffer list
  state.messagesBuffer.addAll(0, bufferedMessages)
}
