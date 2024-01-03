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
import io.infinitic.common.workflows.data.commands.DispatchMethodOnRunningWorkflowCommand
import io.infinitic.common.workflows.data.commands.DispatchNewWorkflowCommand
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethod
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.events.WorkflowCanceledEvent
import io.infinitic.common.workflows.engine.events.WorkflowMethodCanceledEvent
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

internal fun CoroutineScope.cancelWorkflow(
  producer: InfiniticProducer,
  state: WorkflowState,
  message: CancelWorkflow
) {
  launch {
    val deferredList = mutableListOf<Deferred<*>>()

    when (message.workflowMethodId) {
      null -> {
        state.workflowMethods.forEach {
          deferredList.add(
              cancelWorkflowMethod(producer, state, it, message.cancellationReason),
          )
        }

        // clean state
        state.removeWorkflowMethods()
      }

      else -> {
        state.getWorkflowMethod(message.workflowMethodId!!)?.let { workflowMethod ->
          deferredList.add(
              cancelWorkflowMethod(producer, state, workflowMethod, message.cancellationReason),
          )

          // clean state
          state.removeWorkflowMethod(workflowMethod)
        }
      }
    }

    // ensure that WorkflowCanceledEvent is emitted after all WorkflowMethodCanceledEvent
    deferredList.awaitAll()

    val workflowCanceledEvent = WorkflowCanceledEvent(
        workflowName = message.workflowName,
        workflowId = message.workflowId,
        emitterName = EmitterName(producer.name),
        workflowTags = state.workflowTags,
        workflowMeta = state.workflowMeta,
        cancellationReason = message.cancellationReason,
    )

    producer.sendToWorkflowEvents(workflowCanceledEvent)
  }
}

private fun CoroutineScope.cancelWorkflowMethod(
  producer: InfiniticProducer,
  state: WorkflowState,
  workflowMethod: WorkflowMethod,
  cancellationReason: WorkflowCancellationReason
): Deferred<Unit> {
  val emitterName = EmitterName(producer.name)

  // cancel children
  workflowMethod.pastCommands.forEach {
    when (val command = it.command) {
      is DispatchMethodOnRunningWorkflowCommand -> {
        when {
          command.workflowId != null -> launch {
            val cancelWorkflow = CancelWorkflow(
                cancellationReason = WorkflowCancellationReason.CANCELED_BY_PARENT,
                workflowMethodId = WorkflowMethodId.from(it.commandId),
                workflowName = command.workflowName,
                workflowId = command.workflowId!!,
                emitterName = emitterName,
            )
            producer.sendToWorkflowEngine(cancelWorkflow)
          }

          command.workflowTag != null -> launch {
            val cancelWorkflowByTag = CancelWorkflowByTag(
                workflowTag = command.workflowTag!!,
                workflowName = command.workflowName,
                reason = WorkflowCancellationReason.CANCELED_BY_PARENT,
                emitterWorkflowId = state.workflowId,
                emitterName = emitterName,
            )
            producer.sendToWorkflowTag(cancelWorkflowByTag)
          }

          else -> thisShouldNotHappen()
        }
      }

      is DispatchNewWorkflowCommand -> launch {
        val cancelWorkflow = CancelWorkflow(
            cancellationReason = WorkflowCancellationReason.CANCELED_BY_PARENT,
            workflowMethodId = null,
            workflowName = command.workflowName,
            workflowId = WorkflowId.from(it.commandId),
            emitterName = emitterName,
        )
        producer.sendToWorkflowEngine(cancelWorkflow)
      }

      else -> Unit
    }
  }

  return async {
    val workflowMethodCanceledEvent = WorkflowMethodCanceledEvent(
        workflowName = state.workflowName,
        workflowId = state.workflowId,
        workflowMethodId = workflowMethod.workflowMethodId,
        parentWorkflowName = workflowMethod.parentWorkflowName,
        parentWorkflowId = workflowMethod.parentWorkflowId,
        parentWorkflowMethodId = workflowMethod.parentWorkflowMethodId,
        parentClientName = workflowMethod.parentClientName,
        waitingClients = workflowMethod.waitingClients,
        emitterName = emitterName,
        workflowTags = state.workflowTags,
        workflowMeta = state.workflowMeta,
        cancellationReason = cancellationReason,
    )

    producer.sendToWorkflowEvents(workflowMethodCanceledEvent)
  }
}
