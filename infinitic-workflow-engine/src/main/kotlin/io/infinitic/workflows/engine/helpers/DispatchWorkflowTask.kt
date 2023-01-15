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
package io.infinitic.workflows.engine.helpers

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.methodRuns.MethodRunPosition
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.workflows.engine.output.WorkflowEngineOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.dispatchWorkflowTask(
    output: WorkflowEngineOutput,
    state: WorkflowState,
    methodRun: MethodRun,
    methodRunPosition: MethodRunPosition
) {
  state.workflowTaskIndex = state.workflowTaskIndex + 1

  // defines workflow task input
  val workflowTaskParameters =
      WorkflowTaskParameters(
          taskId = TaskId(),
          workflowId = state.workflowId,
          workflowName = state.workflowName,
          workflowVersion = state.workflowVersion,
          workflowTags = state.workflowTags,
          workflowMeta = state.workflowMeta,
          workflowPropertiesHashValue =
              state.propertiesHashValue, // TODO filterStore(state.propertyStore, listOf(methodRun))
          workflowTaskIndex = state.workflowTaskIndex,
          methodRun = methodRun,
          emitterName = output.clientName)

  val dispatchTaskMessage = workflowTaskParameters.toExecuteTaskMessage()

  // dispatch workflow task
  launch { output.sendToWorkflowTaskExecutor(dispatchTaskMessage) }

  with(state) {
    runningWorkflowTaskId = workflowTaskParameters.taskId
    // do not update runningWorkflowTaskInstant if it is a retry
    if (runningMethodRunId != methodRun.methodRunId ||
        runningMethodRunPosition != methodRunPosition) {
      runningWorkflowTaskInstant = MillisInstant.now()
      runningMethodRunId = methodRun.methodRunId
      runningMethodRunPosition = methodRunPosition
    }
  }
}
