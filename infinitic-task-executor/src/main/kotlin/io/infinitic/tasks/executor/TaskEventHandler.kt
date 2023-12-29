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
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.data.TaskReturnValue
import io.infinitic.common.tasks.executors.errors.TaskFailedError
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.TaskCompleted
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.executors.messages.TaskFailed
import io.infinitic.common.tasks.executors.messages.TaskRetried
import io.infinitic.common.tasks.executors.messages.TaskStarted
import io.infinitic.common.tasks.tags.messages.RemoveTagFromTask
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import io.infinitic.common.clients.messages.TaskCompleted as TaskCompletedClient
import io.infinitic.common.clients.messages.TaskFailed as TaskFailedClient
import io.infinitic.common.workflows.engine.messages.TaskCompleted as TaskCompletedWorkflow
import io.infinitic.common.workflows.engine.messages.TaskFailed as TaskFailedWorkflow

class TaskEventHandler(producerAsync: InfiniticProducerAsync) {

  private val logger = KotlinLogging.logger(javaClass.name)
  val producer = LoggedInfiniticProducer(javaClass.name, producerAsync)
  private val emitterName by lazy { EmitterName(producerAsync.name) }

  suspend fun handle(msg: TaskExecutorMessage) {
    msg.logDebug { "received $msg" }

    when (msg) {
      is ExecuteTask -> thisShouldNotHappen()
      is TaskCompleted -> sendTaskCompleted(msg)
      is TaskFailed -> sendTaskFailed(msg)
      is TaskRetried,
      is TaskStarted -> Unit
    }

    msg.logTrace { "processed" }
  }

  private suspend fun sendTaskFailed(msg: TaskFailed): Unit = coroutineScope {

    if (msg.clientName != null) {
      val taskFailed = TaskFailedClient(
          recipientName = msg.clientName!!,
          taskId = msg.taskId,
          cause = msg.executionError,
          emitterName = emitterName,
      )
      launch { producer.sendToClient(taskFailed) }
    }

    if (msg.workflowId != null) {
      val taskFailed = TaskFailedWorkflow(
          workflowName = msg.workflowName ?: thisShouldNotHappen(),
          workflowId = msg.workflowId ?: thisShouldNotHappen(),
          methodRunId = msg.methodRunId ?: thisShouldNotHappen(),
          taskFailedError = TaskFailedError(
              serviceName = msg.serviceName,
              methodName = msg.methodName,
              taskId = msg.taskId,
              cause = msg.executionError,
          ),
          deferredError = msg.deferredError,
          emitterName = emitterName,
      )

      launch { producer.sendLaterToWorkflowEngine(taskFailed) }
    }
  }

  private suspend fun sendTaskCompleted(msg: TaskCompleted) = coroutineScope {
    if (msg.clientName != null) {
      val taskCompleted = TaskCompletedClient(
          recipientName = msg.clientName!!,
          taskId = msg.taskId,
          taskReturnValue = msg.returnValue,
          taskMeta = msg.taskMeta,
          emitterName = emitterName,
      )

      launch { producer.sendToClient(taskCompleted) }
    }

    if (msg.workflowId != null) {
      val taskCompleted = TaskCompletedWorkflow(
          workflowName = msg.workflowName ?: thisShouldNotHappen(),
          workflowId = msg.workflowId ?: thisShouldNotHappen(),
          methodRunId = msg.methodRunId ?: thisShouldNotHappen(),
          taskReturnValue =
          TaskReturnValue(
              serviceName = msg.serviceName,
              taskId = msg.taskId,
              taskMeta = msg.taskMeta,
              returnValue = msg.returnValue,
          ),
          emitterName = emitterName,
      )

      launch { producer.sendLaterToWorkflowEngine(taskCompleted) }
    }

    // remove tags
    msg.taskTags.map {
      val removeTagFromTask = RemoveTagFromTask(
          taskTag = it,
          serviceName = msg.serviceName,
          taskId = msg.taskId,
          emitterName = emitterName,
      )
      launch { producer.sendToTaskTag(removeTagFromTask) }
    }
  }

  private fun TaskExecutorMessage.logDebug(description: () -> String) {
    logger.debug { "$serviceName (${taskId}): ${description()}" }
  }

  private fun TaskExecutorMessage.logTrace(description: () -> String) {
    logger.trace { "$serviceName (${taskId}): ${description()}" }
  }
}