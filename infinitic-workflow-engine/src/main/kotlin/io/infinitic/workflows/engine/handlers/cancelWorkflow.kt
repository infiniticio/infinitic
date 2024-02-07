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
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.transport.WorkflowEngineTopic
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.transport.WorkflowTagTopic
import io.infinitic.common.workflows.data.commands.DispatchMethodOnRunningWorkflowCommand
import io.infinitic.common.workflows.data.commands.DispatchNewWorkflowCommand
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethod
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.data.workflowMethods.awaitingRequesters
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.MethodCanceledEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCanceledEvent
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.cancelWorkflow(
  producer: InfiniticProducer,
  state: WorkflowState,
  message: CancelWorkflow
) = launch {
  val emittedAt = message.emittedAt ?: thisShouldNotHappen()

  coroutineScope {
    when (message.workflowMethodId) {
      null -> {
        state.workflowMethods.forEach {
          launch { cancelWorkflowMethod(producer, state, it, emittedAt) }
        }

        // clean state
        state.removeWorkflowMethods()
      }

      else -> {
        state.getWorkflowMethod(message.workflowMethodId!!)?.let {
          launch { cancelWorkflowMethod(producer, state, it, emittedAt) }
          // clean state
          state.removeWorkflowMethod(it)
        }
      }
    }
  }

  // WorkflowCanceledEvent is emitted after all WorkflowMethodCanceledEvent
  val workflowCanceledEvent = WorkflowCanceledEvent(
      workflowName = message.workflowName,
      workflowId = message.workflowId,
      emitterName = EmitterName(producer.name),
  )
  with(producer) { workflowCanceledEvent.sendTo(WorkflowEventsTopic) }
}


private fun CoroutineScope.cancelWorkflowMethod(
  producer: InfiniticProducer,
  state: WorkflowState,
  workflowMethod: WorkflowMethod,
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
                requester = WorkflowRequester(
                    workflowId = state.workflowId,
                    workflowName = state.workflowName,
                    workflowVersion = state.workflowVersion,
                    workflowMethodName = workflowMethod.methodName,
                    workflowMethodId = workflowMethod.workflowMethodId,
                ),
            )
            with(producer) { cancelWorkflow.sendTo(WorkflowEngineTopic) }
          }

          command.workflowTag != null -> launch {
            val cancelWorkflowByTag = CancelWorkflowByTag(
                workflowTag = command.workflowTag!!,
                workflowName = command.workflowName,
                reason = WorkflowCancellationReason.CANCELED_BY_PARENT,
                emitterWorkflowId = state.workflowId,
                emitterName = emitterName,
                emittedAt = emittedAt,
                requester = WorkflowRequester(
                    workflowId = state.workflowId,
                    workflowName = state.workflowName,
                    workflowVersion = state.workflowVersion,
                    workflowMethodName = workflowMethod.methodName,
                    workflowMethodId = workflowMethod.workflowMethodId,
                ),
            )
            with(producer) { cancelWorkflowByTag.sendTo(WorkflowTagTopic) }
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
            requester = WorkflowRequester(
                workflowId = state.workflowId,
                workflowName = state.workflowName,
                workflowVersion = state.workflowVersion,
                workflowMethodName = workflowMethod.methodName,
                workflowMethodId = workflowMethod.workflowMethodId,
            ),
        )
        with(producer) { cancelWorkflow.sendTo(WorkflowEngineTopic) }
      }

      else -> Unit
    }
  }

  return launch {
    val methodCanceledEvent = MethodCanceledEvent(
        workflowName = state.workflowName,
        workflowId = state.workflowId,
        workflowMethodName = workflowMethod.methodName,
        workflowMethodId = workflowMethod.workflowMethodId,
        awaitingRequesters = workflowMethod.awaitingRequesters,
        emitterName = emitterName,
    )
    with(producer) { methodCanceledEvent.sendTo(WorkflowEventsTopic) }
  }
}
