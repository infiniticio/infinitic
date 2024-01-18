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
package io.infinitic.tasks.executor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.events.messages.TaskFailedEvent
import io.infinitic.common.tasks.events.messages.TaskRetriedEvent
import io.infinitic.common.tasks.events.messages.TaskStartedEvent
import io.infinitic.common.topics.ClientTopic
import io.infinitic.common.topics.WorkflowEngineTopic
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.commands.DispatchMethodOnRunningWorkflowPastCommand
import io.infinitic.common.workflows.data.commands.DispatchNewWorkflowPastCommand
import io.infinitic.common.workflows.data.commands.DispatchTaskPastCommand
import io.infinitic.common.workflows.data.commands.InlineTaskPastCommand
import io.infinitic.common.workflows.data.commands.ReceiveSignalPastCommand
import io.infinitic.common.workflows.data.commands.SendSignalPastCommand
import io.infinitic.common.workflows.data.commands.StartDurationTimerPastCommand
import io.infinitic.common.workflows.data.commands.StartInstantTimerPastCommand
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethodId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskReturnValue
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.tasks.executor.commands.dispatchMethodOnRunningWorkflowCmd
import io.infinitic.tasks.executor.commands.dispatchNewWorkflowCmd
import io.infinitic.tasks.executor.commands.dispatchTaskCmd
import io.infinitic.tasks.executor.commands.sendSignalCmd
import io.infinitic.tasks.executor.commands.startDurationTimerCmd
import io.infinitic.tasks.executor.commands.startInstantTimerCmq
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class TaskEventHandler(producerAsync: InfiniticProducerAsync) {

  private val logger = KotlinLogging.logger(this::class.java.name)
  val producer = LoggedInfiniticProducer(this::class.java.name, producerAsync)
  private val emitterName by lazy { EmitterName(producerAsync.producerName) }

  suspend fun handle(msg: ServiceEventMessage, publishTime: MillisInstant) {
    msg.logDebug { "received $msg" }

    when (msg) {
      is TaskCompletedEvent -> sendTaskCompleted(msg, publishTime)
      is TaskFailedEvent -> sendTaskFailed(msg, publishTime)
      is TaskRetriedEvent,
      is TaskStartedEvent -> Unit
    }

    msg.logTrace { "processed $msg" }
  }

  private suspend fun sendTaskFailed(msg: TaskFailedEvent, publishTime: MillisInstant): Unit =
      coroutineScope {
        // send to parent client
        msg.getEventForClient(emitterName)?.let {
          launch { with(producer) { it.sendTo(ClientTopic) } }
        }
        // send to parent workflow
        msg.getEventForWorkflow(emitterName, publishTime)?.let {
          launch { with(producer) { it.sendTo(WorkflowEngineTopic) } }
        }
      }

  private suspend fun sendTaskCompleted(msg: TaskCompletedEvent, publishTime: MillisInstant) {
    coroutineScope {
      // send to parent client
      msg.getEventForClient(emitterName)?.let {
        launch { with(producer) { it.sendTo(ClientTopic) } }
      }
      // send to parent workflow
      msg.getEventForWorkflow(emitterName, publishTime)?.let {
        launch { with(producer) { it.sendTo(WorkflowEngineTopic) } }
      }
      // remove tags
      msg.getEventsForTag(emitterName).forEach {
        launch { producer.sendToServiceTag(it) }
      }
    }
    // If we are dealing with a workflowTask, we ensure that new commands are dispatched only AFTER
    // the workflow task's completion is forwarded to the engine. This is a safeguard against potential
    // race conditions that may arise if the engine receives the outcomes of the dispatched tasks earlier
    // than the result of the workflowTask.
    if (msg.isWorkflowTask()) completeWorkflowTask(msg, publishTime)
  }

  private suspend fun completeWorkflowTask(msg: TaskCompletedEvent, publishTime: MillisInstant) =
      coroutineScope {

        val result = msg.returnValue.value() as WorkflowTaskReturnValue

        // TODO After 0.13.0, workflowTaskInstant should not be null anymore
        val workflowTaskInstant = result.workflowTaskInstant ?: publishTime

        val currentWorkflow = CurrentWorkflow(
            workflowId = msg.workflowId ?: thisShouldNotHappen(),
            workflowName = msg.workflowName ?: thisShouldNotHappen(),
            workflowMethodId = msg.workflowMethodId ?: thisShouldNotHappen(),
            workflowVersion = result.workflowVersion,
        )

        result.newCommands.forEach {
          when (it) {
            is DispatchNewWorkflowPastCommand ->
              dispatchNewWorkflowCmd(currentWorkflow, it, workflowTaskInstant, producer)

            is DispatchMethodOnRunningWorkflowPastCommand ->
              dispatchMethodOnRunningWorkflowCmd(currentWorkflow, it, workflowTaskInstant, producer)

            is DispatchTaskPastCommand ->
              dispatchTaskCmd(currentWorkflow, it, workflowTaskInstant, producer)

            is SendSignalPastCommand ->
              sendSignalCmd(currentWorkflow, it, workflowTaskInstant, producer)

            is StartDurationTimerPastCommand ->
              startDurationTimerCmd(currentWorkflow, it, workflowTaskInstant, producer)

            is StartInstantTimerPastCommand ->
              startInstantTimerCmq(currentWorkflow, it, producer)

            is ReceiveSignalPastCommand,
            is InlineTaskPastCommand -> Unit // Nothing to do
          }
        }
      }

  data class CurrentWorkflow(
    val workflowId: WorkflowId,
    val workflowName: WorkflowName,
    val workflowMethodId: WorkflowMethodId,
    val workflowVersion: WorkflowVersion,
  )

  private fun ServiceEventMessage.logDebug(description: () -> String) {
    logger.debug { "$serviceName (${taskId}): ${description()}" }
  }

  private fun ServiceEventMessage.logTrace(description: () -> String) {
    logger.trace { "$serviceName (${taskId}): ${description()}" }
  }
}
