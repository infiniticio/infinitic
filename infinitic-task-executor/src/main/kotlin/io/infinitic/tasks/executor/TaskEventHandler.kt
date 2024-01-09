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
import io.infinitic.common.tasks.executors.events.TaskCompletedEvent
import io.infinitic.common.tasks.executors.events.TaskEventMessage
import io.infinitic.common.tasks.executors.events.TaskFailedEvent
import io.infinitic.common.tasks.executors.events.TaskRetriedEvent
import io.infinitic.common.tasks.executors.events.TaskStartedEvent
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.workflows.data.commands.DispatchNewWorkflowPastCommand
import io.infinitic.common.workflows.data.commands.DispatchTaskPastCommand
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskReturnValue
import io.infinitic.tasks.executor.commands.dispatchNewWorkflowCmd
import io.infinitic.tasks.executor.commands.dispatchTaskCmd
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class TaskEventHandler(producerAsync: InfiniticProducerAsync) {

  private val logger = KotlinLogging.logger(javaClass.name)
  val producer = LoggedInfiniticProducer(javaClass.name, producerAsync)
  private val emitterName by lazy { EmitterName(producerAsync.name) }

  @Suppress("UNUSED_PARAMETER")
  fun handle(msg: TaskEventMessage, publishTime: MillisInstant) = producer.run {
    msg.logDebug { "received $msg" }

    when (msg) {
      is TaskCompletedEvent -> sendTaskCompleted(msg)
      is TaskFailedEvent -> sendTaskFailed(msg)
      is TaskRetriedEvent,
      is TaskStartedEvent -> Unit
    }

    msg.logTrace { "processed" }
  }

  private suspend fun sendTaskFailed(msg: TaskFailedEvent): Unit = coroutineScope {
    // send to parent client
    msg.getEventForClient(emitterName)?.let {
      launch { producer.sendToClient(it) }
    }
    // send to parent workflow
    msg.getEventForWorkflow(emitterName)?.let {
      launch { producer.sendToWorkflowEngine(it) }
    }
  }

  private suspend fun sendTaskCompleted(msg: TaskCompletedEvent) = coroutineScope {
    // if workflowTask
    if (msg.isWorkflowTask()) {
      launch { completeWorkflowTask(msg) }
    }
    // send to parent client
    msg.getEventForClient(emitterName)?.let {
      launch { producer.sendToClient(it) }
    }
    // send to parent workflow
    msg.getEventForWorkflow(emitterName)?.let {
      launch { producer.sendToWorkflowEngine(it) }
    }
    // remove tags
    msg.getEventsForTag(emitterName).forEach {
      launch { producer.sendToTaskTag(it) }
    }
  }

  private suspend fun completeWorkflowTask(msg: TaskCompletedEvent) = coroutineScope {
    val result = msg.returnValue.value() as WorkflowTaskReturnValue

    result.newCommands.forEach {
      when (it) {
        is DispatchTaskPastCommand -> dispatchTaskCmd(msg, it, producer)
        is DispatchNewWorkflowPastCommand -> dispatchNewWorkflowCmd(msg, it, producer)
//        is DispatchMethodOnRunningWorkflowPastCommand -> dispatchMethodOnRunningWorkflowCmd(
//            it,
//            state,
//            producer,
//            bufferedMessages,
//        )
//
//        is SendSignalPastCommand -> sendSignalCmd(it, state, producer, bufferedMessages)
//        is InlineTaskPastCommand -> Unit // Nothing to do
//        is StartDurationTimerPastCommand -> startDurationTimerCmd(it, state, producer)
//        is StartInstantTimerPastCommand -> startInstantTimerCmq(it, state, producer)
//        is ReceiveSignalPastCommand -> receiveSignalCmd(it, state)
        else -> Unit
      }
    }
  }

  private fun TaskEventMessage.logDebug(description: () -> String) {
    logger.debug { "$serviceName (${taskId}): ${description()}" }
  }

  private fun TaskEventMessage.logTrace(description: () -> String) {
    logger.trace { "$serviceName (${taskId}): ${description()}" }
  }
}
