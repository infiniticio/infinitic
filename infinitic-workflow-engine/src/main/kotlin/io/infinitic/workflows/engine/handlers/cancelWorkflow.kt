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

import io.infinitic.common.data.MillisInstant
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

internal fun CoroutineScope.cancelWorkflow(
  producer: InfiniticProducer,
  state: WorkflowState,
  message: CancelWorkflow
) {
  launch {
    val jobs = mutableListOf<Job>()
    val emittedAt = message.emittedAt ?: thisShouldNotHappen()

    when (message.workflowMethodId) {
      null -> {
        state.workflowMethods.forEach {
          jobs.add(
              cancelWorkflowMethod(producer, state, it, message.cancellationReason, emittedAt),
          )
        }

        // clean state
        state.removeWorkflowMethods()
      }

      else -> {
        state.getWorkflowMethod(message.workflowMethodId!!)?.let {
          jobs.add(
              cancelWorkflowMethod(producer, state, it, message.cancellationReason, emittedAt),
          )
          // clean state
          state.removeWorkflowMethod(it)
        }
      }
    }

    // ensure that WorkflowCanceledEvent is emitted after all WorkflowMethodCanceledEvent
    jobs.joinAll()

    val workflowCanceledEvent = WorkflowCanceledEvent(
        workflowName = message.workflowName,
        workflowId = message.workflowId,
        workflowTags = state.workflowTags,
        workflowMeta = state.workflowMeta,
        cancellationReason = message.cancellationReason,
        emitterName = EmitterName(producer.name),
    )

    producer.sendToWorkflowEvents(workflowCanceledEvent)
  }
}

private fun CoroutineScope.cancelWorkflowMethod(
  producer: InfiniticProducer,
  state: WorkflowState,
  workflowMethod: WorkflowMethod,
  cancellationReason: WorkflowCancellationReason,
  emittedAt: MillisInstant
): Job {
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
                emittedAt = emittedAt,
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
                emittedAt = emittedAt,
            )
            producer.sendToWorkflowTag(cancelWorkflowByTag)
          }

          else -> thisShouldNotHappen()
        }
      }

      is DispatchNewWorkflowCommand -> launch {
        val cancelWorkflow = CancelWorkflow(
            workflowId = WorkflowId.from(it.commandId),
            workflowName = command.workflowName,
            workflowMethodId = null,
            cancellationReason = WorkflowCancellationReason.CANCELED_BY_PARENT,
            emitterName = emitterName,
            emittedAt = emittedAt,
        )
        producer.sendToWorkflowEngine(cancelWorkflow)
      }

      else -> Unit
    }
  }

  return launch {
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
