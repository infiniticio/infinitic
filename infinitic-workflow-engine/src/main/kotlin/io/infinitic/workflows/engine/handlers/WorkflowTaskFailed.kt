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
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.executors.errors.FailedWorkflowError
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.engine.messages.ChildMethodFailed
import io.infinitic.common.workflows.engine.messages.TaskFailed
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.workflows.engine.output.WorkflowEngineOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.workflowTaskFailed(
    output: WorkflowEngineOutput,
    state: WorkflowState,
    message: TaskFailed
): MutableList<WorkflowEngineMessage> {
  val methodRun: MethodRun = state.getRunningMethodRun()

  val deferredError =
      when (val deferredError = message.deferredError) {
        null -> message.failedTaskError
        else -> deferredError
      }

  // send to waiting clients
  methodRun.waitingClients.forEach {
    val methodFailed =
        MethodFailed(
            recipientName = it,
            workflowId = state.workflowId,
            methodRunId = methodRun.methodRunId,
            cause = deferredError,
            emitterName = output.clientName)
    launch { output.sendEventsToClient(methodFailed) }
  }
  methodRun.waitingClients.clear()

  val bufferedMessages = mutableListOf<WorkflowEngineMessage>()

  // send to parent workflow
  methodRun.parentWorkflowId?.let {
    val childMethodFailed =
        ChildMethodFailed(
            workflowId = it,
            workflowName = methodRun.parentWorkflowName ?: thisShouldNotHappen(),
            methodRunId = methodRun.parentMethodRunId ?: thisShouldNotHappen(),
            childFailedWorkflowError =
                FailedWorkflowError(
                    workflowName = state.workflowName,
                    workflowId = state.workflowId,
                    methodName = methodRun.methodName,
                    methodRunId = methodRun.methodRunId,
                    deferredError = deferredError),
            emitterName = output.clientName)
    if (it == state.workflowId) {
      // case of method dispatched within same workflow
      bufferedMessages.add(childMethodFailed)
    } else {
      launch { output.sendToWorkflowEngine(childMethodFailed) }
    }
  }

  return bufferedMessages
}
