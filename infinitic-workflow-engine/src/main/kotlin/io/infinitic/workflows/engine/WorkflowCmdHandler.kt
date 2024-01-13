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
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.engine.messages.DispatchNewWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class WorkflowCmdHandler(producerAsync: InfiniticProducerAsync) {

  private val logger = KotlinLogging.logger(javaClass.name)
  val producer = LoggedInfiniticProducer(javaClass.name, producerAsync)
  val emitterName by lazy { EmitterName(producer.name) }

  suspend fun handle(msg: WorkflowEngineMessage, publishTime: MillisInstant) {
    msg.logDebug { "received $msg" }

    // define emittedAt from the publishing instant if not yet defined
    msg.emittedAt = msg.emittedAt ?: publishTime

    when (msg) {
      is DispatchNewWorkflow -> dispatchNewWorkflow(msg, publishTime)
      else -> producer.sendToWorkflowEngine(msg)
    }

    msg.logTrace { "processed $msg" }
  }

  // We dispatch a workflow task right away
  // This is done to accelerate the processing in case of burst
  private suspend fun dispatchNewWorkflow(msg: DispatchNewWorkflow, publishTime: MillisInstant) =
      coroutineScope {

        val dispatchNewWorkflow = msg.copy(workflowTaskId = TaskId())

        // first we send to workflow-engine
        producer.sendToWorkflowEngine(dispatchNewWorkflow)

        // only after, we trigger the workflow-task
        launch {
          // defines workflow task input
          val workflowTaskParameters = WorkflowTaskParameters(
              taskId = dispatchNewWorkflow.workflowTaskId!!,
              workflowId = dispatchNewWorkflow.workflowId,
              workflowName = dispatchNewWorkflow.workflowName,
              workflowVersion = null,
              workflowTags = dispatchNewWorkflow.workflowTags,
              workflowMeta = dispatchNewWorkflow.workflowMeta,
              workflowPropertiesHashValue = mutableMapOf(),
              workflowTaskIndex = WorkflowTaskIndex(1),
              workflowMethod = dispatchNewWorkflow.workflowMethod(),
              workflowTaskInstant = msg.emittedAt ?: publishTime,
              emitterName = emitterName,
          )

          val dispatchTaskMessage = workflowTaskParameters.toExecuteTaskMessage()

          // dispatch workflow task
          producer.sendToTaskExecutor(dispatchTaskMessage)
        }

        // the 2 events are sent sequentially, to ensure they have consistent timestamps
        // (workflowStarted before workflowMethodStarted)
        launch {
          producer.sendToWorkflowEvents(dispatchNewWorkflow.workflowStartedEvent(emitterName))
          producer.sendToWorkflowEvents(dispatchNewWorkflow.workflowMethodStartedEvent(emitterName))
        }
      }

  private fun WorkflowEngineMessage.logDebug(description: () -> String) {
    logger.debug { "$workflowName (${workflowId}): ${description()}" }
  }

  private fun WorkflowEngineMessage.logTrace(description: () -> String) {
    logger.trace { "$workflowName (${workflowId}): ${description()}" }
  }
}
