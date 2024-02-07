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
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.tasks.executors.errors.TaskTimedOutError
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.tags.messages.AddTaskIdToTag
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.DelayedWorkflowEngineTopic
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagTopic
import io.infinitic.common.transport.WorkflowEngineTopic
import io.infinitic.common.workflows.engine.messages.DurationTimerDescription
import io.infinitic.common.workflows.engine.messages.InstantTimerDescription
import io.infinitic.common.workflows.engine.messages.MethodCanceledEvent
import io.infinitic.common.workflows.engine.messages.MethodCommandedEvent
import io.infinitic.common.workflows.engine.messages.MethodCompletedEvent
import io.infinitic.common.workflows.engine.messages.MethodFailedEvent
import io.infinitic.common.workflows.engine.messages.MethodTimedOutEvent
import io.infinitic.common.workflows.engine.messages.RemoteMethodDispatchedEvent
import io.infinitic.common.workflows.engine.messages.RemoteSignalDispatchedEvent
import io.infinitic.common.workflows.engine.messages.RemoteTaskTimedOut
import io.infinitic.common.workflows.engine.messages.RemoteTimerCompleted
import io.infinitic.common.workflows.engine.messages.TaskDispatchedEvent
import io.infinitic.common.workflows.engine.messages.TimerDispatchedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCanceledEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCompletedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class WorkflowEventHandler(producerAsync: InfiniticProducerAsync) {

  private val logger = KotlinLogging.logger(this::class.java.name)
  val producer = LoggedInfiniticProducer(this::class.java.name, producerAsync)

  val emitterName by lazy { EmitterName(producer.name) }

  suspend fun handle(msg: WorkflowEventMessage, publishTime: MillisInstant) {
    msg.logDebug { "received $msg" }

    when (msg) {
      is WorkflowCanceledEvent -> Unit
      is WorkflowCompletedEvent -> Unit
      is MethodCommandedEvent -> Unit
      is MethodCanceledEvent -> sendWorkflowMethodCanceled(msg, publishTime)
      is MethodCompletedEvent -> sendWorkflowMethodCompleted(msg, publishTime)
      is MethodFailedEvent -> sendWorkflowMethodFailed(msg, publishTime)
      is MethodTimedOutEvent -> sendWorkflowMethodTimedOut(msg, publishTime)
      is TaskDispatchedEvent -> sendTask(msg)
      is RemoteMethodDispatchedEvent -> Unit
      is TimerDispatchedEvent -> sendTimer(msg)
      is RemoteSignalDispatchedEvent -> TODO()
    }

    msg.logTrace { "processed" }
  }

  private suspend fun sendTask(
    msg: TaskDispatchedEvent,
  ) {
    val executeTask = with(msg.taskDispatched) {
      ExecuteTask(
          serviceName = serviceName,
          taskId = taskId,
          taskRetrySequence = TaskRetrySequence(0),
          taskRetryIndex = TaskRetryIndex(0),
          requester = msg.requester(),
          taskTags = taskTags,
          taskMeta = taskMeta,
          clientWaiting = false,
          methodName = methodName,
          methodParameterTypes = methodParameterTypes,
          methodParameters = methodParameters,
          lastError = null,
          emitterName = emitterName,
      )
    }

    coroutineScope {
      // start task
      launch { with(producer) { executeTask.sendTo(ServiceExecutorTopic) } }

      // add provided tags
      executeTask.taskTags.forEach {
        launch {
          val addTaskIdToTag = AddTaskIdToTag(
              serviceName = executeTask.serviceName,
              taskTag = it,
              taskId = executeTask.taskId,
              emitterName = emitterName,
          )
          with(producer) { addTaskIdToTag.sendTo(ServiceTagTopic) }
        }
      }

      // send task timeout if any
      msg.taskDispatched.timeout?.let {
        launch {
          val remoteTaskTimedOut = RemoteTaskTimedOut(
              taskTimedOutError = TaskTimedOutError(
                  serviceName = executeTask.serviceName,
                  taskId = executeTask.taskId,
                  methodName = executeTask.methodName,
              ),
              workflowName = msg.workflowName,
              workflowId = msg.workflowId,
              workflowVersion = msg.workflowVersion,
              workflowMethodName = msg.workflowMethodName,
              workflowMethodId = msg.workflowMethodId,
              emitterName = emitterName,
              emittedAt = msg.taskDispatched.emittedAt + it,
          )
          with(producer) { remoteTaskTimedOut.sendTo(DelayedWorkflowEngineTopic, it) }
        }
      }
    }
  }

  private suspend fun sendTimer(
    msg: TimerDispatchedEvent,
  ) {
    val timer = msg.timerDispatched
    val timerInstant = when (timer) {
      is InstantTimerDescription -> timer.timerInstant
      is DurationTimerDescription -> timer.emittedAt + timer.duration
    }

    val remoteTimerCompleted = RemoteTimerCompleted(
        timerId = timer.timerId,
        workflowName = msg.workflowName,
        workflowId = msg.workflowId,
        workflowVersion = msg.workflowVersion,
        workflowMethodName = msg.workflowMethodName,
        workflowMethodId = msg.workflowMethodId,
        emitterName = emitterName,
        emittedAt = timerInstant,
    )

    // todo: Check if there is a way not to use MillisInstant.now()
    val delay = timerInstant - MillisInstant.now()
    with(producer) { remoteTimerCompleted.sendTo(DelayedWorkflowEngineTopic, delay) }
  }

  private suspend fun sendWorkflowMethodCanceled(
    msg: MethodCanceledEvent,
    publishTime: MillisInstant
  ) = coroutineScope {
    // tell awaiting clients
    msg.getEventForAwaitingClients(emitterName).forEach { event ->
      launch { with(producer) { event.sendTo(ClientTopic) } }
    }

    // tell awaiting workflow (except itself)
    msg.getEventForAwaitingWorkflows(emitterName, publishTime)
        .filter { it.workflowId != msg.workflowId }.forEach { event ->
          launch { with(producer) { event.sendTo(WorkflowEngineTopic) } }
        }
  }

  private suspend fun sendWorkflowMethodCompleted(
    msg: MethodCompletedEvent,
    publishTime: MillisInstant
  ) = coroutineScope {
    // tell awaiting clients
    msg.getEventForAwaitingClients(emitterName).forEach { event ->
      launch { with(producer) { event.sendTo(ClientTopic) } }
    }

    // tell awaiting workflow (except itself)
    msg.getEventForAwaitingWorkflows(emitterName, publishTime)
        .filter { it.workflowId != msg.workflowId }.forEach { event ->
          launch { with(producer) { event.sendTo(WorkflowEngineTopic) } }
        }
  }

  private suspend fun sendWorkflowMethodFailed(
    msg: MethodFailedEvent,
    publishTime: MillisInstant
  ) = coroutineScope {
    // tell awaiting clients
    msg.getEventForAwaitingClients(emitterName).forEach { event ->
      launch { with(producer) { event.sendTo(ClientTopic) } }
    }

    // tell awaiting workflow (except itself)
    msg.getEventForAwaitingWorkflows(emitterName, publishTime)
        .filter { it.workflowId != msg.workflowId }.forEach { event ->
          launch { with(producer) { event.sendTo(WorkflowEngineTopic) } }
        }
  }

  private suspend fun sendWorkflowMethodTimedOut(
    msg: MethodTimedOutEvent,
    publishTime: MillisInstant
  ) = coroutineScope {
    // tell awaiting clients
    msg.getEventForAwaitingClients(emitterName).forEach { event ->
      launch { with(producer) { event.sendTo(ClientTopic) } }
    }

    // tell awaiting workflow (except itself)
    msg.getEventForAwaitingWorkflows(emitterName, publishTime)
        .filter { it.workflowId != msg.workflowId }.forEach { event ->
          launch { with(producer) { event.sendTo(WorkflowEngineTopic) } }
        }
  }

  private fun WorkflowEventMessage.logDebug(description: () -> String) {
    logger.debug { "$workflowName (${workflowId}): ${description()}" }
  }

  private fun WorkflowEventMessage.logTrace(description: () -> String) {
    logger.trace { "$workflowName (${workflowId}): ${description()}" }
  }
}
