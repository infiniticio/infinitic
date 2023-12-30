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

import io.infinitic.common.clients.messages.MethodCanceled
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.executors.errors.MethodCanceledError
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.data.commands.DispatchExistingWorkflowCommand
import io.infinitic.common.workflows.data.commands.DispatchNewWorkflowCommand
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethod
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.ChildMethodCanceled
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.cancelWorkflow(
  producer: InfiniticProducer,
  state: WorkflowState,
  message: CancelWorkflow
) {
  when (message.workflowMethodId) {
    null -> {
      state.workflowMethods.forEach { cancelWorkflowMethod(producer, state, it, message.reason) }

      // clean state
      state.removeWorkflowMethods()
    }

    else -> {
      state.getMethodRun(message.workflowMethodId!!)?.let { methodRun ->
        cancelWorkflowMethod(producer, state, methodRun, message.reason)

        // clean state
        state.removeWorkflowMethod(methodRun)
      }
    }
  }
}

private fun CoroutineScope.cancelWorkflowMethod(
  producer: InfiniticProducer,
  state: WorkflowState,
  methodRun: WorkflowMethod,
  reason: WorkflowCancellationReason
) {
  val emitterName = EmitterName(producer.name)

  // inform waiting clients of cancellation
  methodRun.waitingClients.forEach {
    val workflowCanceled = MethodCanceled(
        recipientName = it,
        workflowId = state.workflowId,
        workflowMethodId = methodRun.workflowMethodId,
        emitterName = emitterName,
    )
    launch { producer.sendToClient(workflowCanceled) }
  }
  methodRun.waitingClients.clear()

  // inform parents of cancellation (if parent did not trigger the cancellation!)
  if (reason != WorkflowCancellationReason.CANCELED_BY_PARENT &&
    methodRun.parentWorkflowId != null) {
    val childMethodCanceled = ChildMethodCanceled(
        childMethodCanceledError =
        MethodCanceledError(
            workflowName = state.workflowName,
            workflowId = state.workflowId,
            workflowMethodId = methodRun.workflowMethodId,
        ),
        workflowName = methodRun.parentWorkflowName ?: thisShouldNotHappen(),
        workflowId = methodRun.parentWorkflowId!!,
        workflowMethodId = methodRun.parentWorkflowMethodId ?: thisShouldNotHappen(),
        emitterName = emitterName,
    )
    launch { producer.sendLaterToWorkflowEngine(childMethodCanceled) }
  }

  // cancel children
  methodRun.pastCommands.forEach {
    when (val command = it.command) {
      is DispatchExistingWorkflowCommand -> {
        when {
          command.workflowId != null -> {
            val cancelWorkflow = CancelWorkflow(
                reason = WorkflowCancellationReason.CANCELED_BY_PARENT,
                workflowMethodId = WorkflowMethodId.from(it.commandId),
                workflowName = command.workflowName,
                workflowId = command.workflowId!!,
                emitterName = emitterName,
            )
            launch { producer.sendLaterToWorkflowEngine(cancelWorkflow) }
          }

          command.workflowTag != null -> {
            val cancelWorkflowByTag = CancelWorkflowByTag(
                workflowTag = command.workflowTag!!,
                workflowName = command.workflowName,
                reason = WorkflowCancellationReason.CANCELED_BY_PARENT,
                emitterWorkflowId = state.workflowId,
                emitterName = emitterName,
            )
            launch { producer.sendToWorkflowTag(cancelWorkflowByTag) }
          }

          else -> thisShouldNotHappen()
        }
      }

      is DispatchNewWorkflowCommand -> {
        val cancelWorkflow = CancelWorkflow(
            reason = WorkflowCancellationReason.CANCELED_BY_PARENT,
            workflowMethodId = null,
            workflowName = command.workflowName,
            workflowId = WorkflowId.from(it.commandId),
            emitterName = emitterName,
        )
        launch { producer.sendLaterToWorkflowEngine(cancelWorkflow) }
      }

      else -> {
        // TODO check this
        // thisShouldNotHappen()
      }
    }
  }
}
