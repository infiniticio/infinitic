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
import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.tasks.executors.errors.ExecutionError
import io.infinitic.common.tasks.executors.errors.WorkflowTaskFailedError
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.workers.data.WorkerName
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
  val emittedAt = state.runningWorkflowTaskInstant ?: thisShouldNotHappen()

  val workflowMethod = state.getRunningWorkflowMethod()

  val deferredError: DeferredError = when (val deferredError = message.deferredError) {
    // an Exception has thrown in the workflow task
    null -> WorkflowTaskFailedError(
        workflowName = message.workflowName,
        workflowId = message.workflowId,
        workflowTaskId = message.taskId(),
        cause = with(message.taskFailedError.cause) {
          ExecutionError(
              workerName = WorkerName.from(message.emitterName),
              name = name,
              message = this.message,
              stackTraceToString = stackTraceToString,
              cause = cause,
          )
        },
    )
    // a deferred Exception has thrown in the workflow task
    else -> deferredError
  }

  val methodFailedEvent = MethodFailedEvent(
      workflowName = state.workflowName,
      workflowId = state.workflowId,
      workflowVersion = state.workflowVersion,
      workflowMethodId = workflowMethod.workflowMethodId,
      workflowMethodName = workflowMethod.methodName,
      awaitingRequesters = workflowMethod.awaitingRequesters,
      emitterName = EmitterName.BUFFERED,
      deferredError = deferredError,
  )
  launch {
    with(producer) {
      methodFailedEvent.copy(emitterName = EmitterName(producer.getProducerName()))
          .sendTo(WorkflowStateEventTopic)
    }
  }

  // send info to itself by adding a fake message on messageBuffer
  methodFailedEvent.getEventForAwaitingWorkflows(EmitterName.BUFFERED, emittedAt)
      .firstOrNull { it.workflowId == message.workflowId }
      ?.let { state.messagesBuffer.add(0, it) }
}

