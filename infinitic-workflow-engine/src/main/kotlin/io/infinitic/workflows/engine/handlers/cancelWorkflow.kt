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
import io.infinitic.common.data.ClientName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.executors.errors.MethodCanceledError
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.data.commands.DispatchExistingWorkflowCommand
import io.infinitic.common.workflows.data.commands.DispatchNewWorkflowCommand
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
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
  when (message.methodRunId) {
    null -> {
      state.methodRuns.forEach { cancelMethodRun(producer, state, it, message.reason) }

      // clean state
      state.removeMethodRuns()
    }

    else -> {
      state.getMethodRun(message.methodRunId!!)?.let { methodRun ->
        cancelMethodRun(producer, state, methodRun, message.reason)

        // clean state
        state.removeMethodRun(methodRun)
      }
    }
  }
}

private fun CoroutineScope.cancelMethodRun(
  producer: InfiniticProducer,
  state: WorkflowState,
  methodRun: MethodRun,
  reason: WorkflowCancellationReason
) {
  // inform waiting clients of cancellation
  methodRun.waitingClients.forEach {
    val workflowCanceled =
        MethodCanceled(
            recipientName = it,
            workflowId = state.workflowId,
            methodRunId = methodRun.methodRunId,
            emitterName = ClientName(producer.name),
        )
    launch { producer.send(workflowCanceled) }
  }
  methodRun.waitingClients.clear()

  // inform parents of cancellation (if parent did not trigger the cancellation!)
  if (reason != WorkflowCancellationReason.CANCELED_BY_PARENT &&
    methodRun.parentWorkflowId != null) {
    val childMethodCanceled =
        ChildMethodCanceled(
            workflowId = methodRun.parentWorkflowId!!,
            workflowName = methodRun.parentWorkflowName ?: thisShouldNotHappen(),
            methodRunId = methodRun.parentMethodRunId ?: thisShouldNotHappen(),
            childMethodCanceledError =
            MethodCanceledError(
                workflowName = state.workflowName,
                workflowId = state.workflowId,
                methodRunId = methodRun.methodRunId,
            ),
            emitterName = ClientName(producer.name),
        )
    launch { producer.send(childMethodCanceled) }
  }

  // cancel children
  methodRun.pastCommands.forEach {
    when (val command = it.command) {
      is DispatchExistingWorkflowCommand -> {
        when {
          command.workflowId != null -> {
            val cancelWorkflow =
                CancelWorkflow(
                    workflowId = command.workflowId!!,
                    workflowName = command.workflowName,
                    methodRunId = MethodRunId.from(it.commandId),
                    reason = WorkflowCancellationReason.CANCELED_BY_PARENT,
                    emitterName = ClientName(producer.name),
                )
            launch { producer.send(cancelWorkflow) }
          }

          command.workflowTag != null -> {
            val cancelWorkflowByTag =
                CancelWorkflowByTag(
                    workflowTag = command.workflowTag!!,
                    workflowName = command.workflowName,
                    reason = WorkflowCancellationReason.CANCELED_BY_PARENT,
                    emitterWorkflowId = state.workflowId,
                    emitterName = ClientName(producer.name),
                )
            launch { producer.send(cancelWorkflowByTag) }
          }

          else -> thisShouldNotHappen()
        }
      }

      is DispatchNewWorkflowCommand -> {
        val cancelWorkflow =
            CancelWorkflow(
                workflowId = WorkflowId.from(it.commandId),
                workflowName = command.workflowName,
                methodRunId = null,
                reason = WorkflowCancellationReason.CANCELED_BY_PARENT,
                emitterName = ClientName(producer.name),
            )
        launch { producer.send(cancelWorkflow) }
      }

      else -> {
        // TODO check this
        // thisShouldNotHappen()
      }
    }
  }
}
