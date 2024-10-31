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
package io.infinitic.workflows.engine

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.utils.IdGenerator
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.engine.commands.dispatchTask
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowStateCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.common.workflows.engine.messages.requester
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class WorkflowStateCmdHandler(val producer: InfiniticProducer) {

  private suspend fun getEmitterName() = EmitterName(producer.getName())

  suspend fun batchProcess(
    messages: List<WorkflowStateCmdMessage>,
    publishTimes: List<MillisInstant>
  ) = coroutineScope {
    messages.zip(publishTimes) { msg, publishTime -> launch { process(msg, publishTime) } }
  }

  suspend fun process(msg: WorkflowStateEngineMessage, publishTime: MillisInstant) {
    // define emittedAt from the publishing instant if not yet defined
    msg.emittedAt = msg.emittedAt ?: publishTime

    when (msg) {
      is DispatchWorkflow -> dispatchNewWorkflow(msg, publishTime)
      else -> with(producer) { msg.sendTo(WorkflowStateEngineTopic) }
    }
  }

  // We dispatch a workflow task right away
  // This is done to accelerate the processing in case of burst
  private suspend fun dispatchNewWorkflow(msg: DispatchWorkflow, publishTime: MillisInstant) =
      coroutineScope {

        // first we forward the message to workflow-engine
        val dispatchNewWorkflow = msg.copy(
            workflowTaskId = TaskId(
                // Deterministic id creation. Without it, an issue arises if dispatchNewWorkflow fails
                // just after having forwarded the dispatchNewWorkflow message to the Engine,
                // that will then await for a workflowTaskId that will never come
                IdGenerator.from(msg.emittedAt!!, "workflowId=${msg.workflowId}"),
            ),
        )
        with(producer) { dispatchNewWorkflow.sendTo(WorkflowStateEngineTopic) }

        // The workflowTask is sent only after the previous message,
        // to prevent a possible race condition where the outcome of the workflowTask
        // commands arrives before the engine is made aware of them by the previous message.
        launch {
          // defines workflow task input
          val workflowTaskParameters = with(dispatchNewWorkflow) {
            WorkflowTaskParameters(
                taskId = workflowTaskId!!,
                workflowId = workflowId,
                workflowName = workflowName,
                workflowVersion = null,
                workflowTags = workflowTags,
                workflowMeta = workflowMeta,
                workflowPropertiesHashValue = mutableMapOf(),
                workflowTaskIndex = WorkflowTaskIndex(1),
                workflowMethod = workflowMethod(),
                workflowTaskInstant = msg.emittedAt ?: publishTime,
                emitterName = emitterName,
            )
          }

          val taskDispatchedEvent =
              workflowTaskParameters.workflowTaskDispatchedEvent(getEmitterName())

          with(producer) {
            // dispatch workflow task
            dispatchTask(taskDispatchedEvent.taskDispatched, taskDispatchedEvent.requester)
            // dispatch workflow event
            taskDispatchedEvent.sendTo(WorkflowStateEventTopic)
          }


          with(producer) {
            // event: starting new method
            dispatchNewWorkflow.methodCommandedEvent(getEmitterName())
                .sendTo(WorkflowStateEventTopic)
          }
        }
      }

  companion object {
    val logger = KotlinLogging.logger { }
  }
}
