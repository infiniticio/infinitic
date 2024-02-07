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
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.utils.IdGenerator
import io.infinitic.common.workflows.data.workflowMethods.PositionInWorkflowMethod
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethod
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.engine.state.WorkflowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.dispatchWorkflowTask(
  producer: InfiniticProducer,
  state: WorkflowState,
  workflowMethod: WorkflowMethod,
  positionInMethod: PositionInWorkflowMethod,
  workflowTaskInstant: MillisInstant
) {
  val emitterName = EmitterName(producer.name)

  // next workflow task
  state.workflowTaskIndex += 1

  // Deterministic generation of workflowTaskInstant
  val seed = "workflowId=${state.workflowId}" +
      "workflowVersion=${state.workflowVersion}" +
      "workflowMethodId=${workflowMethod.workflowMethodId}" +
      "positionInMethod=$positionInMethod"
  val workflowTaskId = TaskId(IdGenerator.from(workflowTaskInstant, seed))

  launch {
    // defines workflow task input
    val workflowTaskParameters = WorkflowTaskParameters(
        taskId = workflowTaskId,
        workflowId = state.workflowId,
        workflowName = state.workflowName,
        workflowVersion = state.workflowVersion,
        workflowTags = state.workflowTags,
        workflowMeta = state.workflowMeta,
        workflowPropertiesHashValue = state.propertiesHashValue, // TODO filterStore(state.propertyStore, listOf(methodRun))
        workflowTaskIndex = state.workflowTaskIndex,
        workflowTaskInstant = workflowTaskInstant,
        workflowMethod = workflowMethod,
        emitterName = emitterName,
    )

    // dispatch workflow task
    val taskDispatchedEvent = workflowTaskParameters.workflowTaskDispatchedEvent(emitterName)
    with(producer) { taskDispatchedEvent.sendTo(WorkflowEventsTopic) }
  }

  // update runningWorkflowTask
  state.runningWorkflowTaskId = workflowTaskId
  state.runningWorkflowTaskInstant = workflowTaskInstant
  state.runningWorkflowMethodId = workflowMethod.workflowMethodId
  state.positionInRunningWorkflowMethod = positionInMethod
}
