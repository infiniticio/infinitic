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
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.MethodRunUnknown
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.executors.errors.MethodUnknownError
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.ChildMethodCanceled
import io.infinitic.common.workflows.engine.messages.ChildMethodCompleted
import io.infinitic.common.workflows.engine.messages.ChildMethodFailed
import io.infinitic.common.workflows.engine.messages.ChildMethodTimedOut
import io.infinitic.common.workflows.engine.messages.ChildMethodUnknown
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.CompleteWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchMethodOnRunningWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchNewWorkflow
import io.infinitic.common.workflows.engine.messages.MethodEvent
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.TaskCanceled
import io.infinitic.common.workflows.engine.messages.TaskCompleted
import io.infinitic.common.workflows.engine.messages.TaskEvent
import io.infinitic.common.workflows.engine.messages.TaskFailed
import io.infinitic.common.workflows.engine.messages.TaskTimedOut
import io.infinitic.common.workflows.engine.messages.TimerCompleted
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEvent
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.workflows.engine.handlers.cancelWorkflow
import io.infinitic.workflows.engine.handlers.childMethodCanceled
import io.infinitic.workflows.engine.handlers.childMethodCompleted
import io.infinitic.workflows.engine.handlers.childMethodFailed
import io.infinitic.workflows.engine.handlers.childMethodTimedOut
import io.infinitic.workflows.engine.handlers.childMethodUnknown
import io.infinitic.workflows.engine.handlers.completeTimer
import io.infinitic.workflows.engine.handlers.dispatchMethod
import io.infinitic.workflows.engine.handlers.dispatchWorkflow
import io.infinitic.workflows.engine.handlers.retryTasks
import io.infinitic.workflows.engine.handlers.retryWorkflowTask
import io.infinitic.workflows.engine.handlers.sendSignal
import io.infinitic.workflows.engine.handlers.taskCanceled
import io.infinitic.workflows.engine.handlers.taskCompleted
import io.infinitic.workflows.engine.handlers.taskFailed
import io.infinitic.workflows.engine.handlers.taskTimedOut
import io.infinitic.workflows.engine.handlers.timerCompleted
import io.infinitic.workflows.engine.handlers.waitWorkflow
import io.infinitic.workflows.engine.handlers.workflowTaskCompleted
import io.infinitic.workflows.engine.handlers.workflowTaskFailed
import io.infinitic.workflows.engine.helpers.removeTags
import io.infinitic.workflows.engine.storage.LoggedWorkflowStateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class WorkflowEngine(
  storage: WorkflowStateStorage,
  producerAsync: InfiniticProducerAsync
) {
  companion object {
    const val NO_STATE_DISCARDING_REASON = "for having null workflow state"
  }

  private val logger = KotlinLogging.logger(javaClass.name)

  private val storage = LoggedWorkflowStateStorage(javaClass.name, storage)

  private val producer = LoggedInfiniticProducer(javaClass.name, producerAsync)

  private val emitterName by lazy { EmitterName(producer.name) }

  suspend fun handle(message: WorkflowEngineMessage) {
    logDebug(message) { "Receiving $message" }

    // set producer id for logging purpose
    // this works as a workflow engine instance process only one message at a time
    producer.id = message.workflowId.toString()

    // get current state
    var state = storage.getState(message.workflowId)

    // if a crash happened between the state update and the message acknowledgement,
    // it's possible to receive a message that has already been processed
    // in this case, we discard it
    if (state?.lastMessageId == message.messageId) {
      logDiscarding(message) { "as state already contains this messageId" }

      return
    }

    state = when (state) {
      null -> processMessageWithoutState(message)
      else -> processMessageWithState(message, state)
    } ?: return // returning null means that we can stop here

    when (state.methodRuns.size) {
      // workflow is completed
      0 -> {
        // remove reference to this workflow in tags
        removeTags(producer, state)
        // delete state
        storage.delState(message.workflowId)
      }
      // workflow is ongoing
      else -> {
        // record this message as the last processed
        state.lastMessageId = message.messageId
        // update state
        storage.putState(message.workflowId, state)
      }
    }
  }

  private suspend fun processMessageWithoutState(
    message: WorkflowEngineMessage
  ): WorkflowState? = coroutineScope {
    // New workflow to dispatch
    if (message is DispatchNewWorkflow) {
      return@coroutineScope dispatchWorkflow(producer, message)
    }

    // targeted workflow is not found, we tell the message emitter
    when (message) {
      // a client wants to dispatch a method on the missing workflow
      is DispatchMethodOnRunningWorkflow -> {
        if (message.clientWaiting) {
          val methodRunUnknown = MethodRunUnknown(
              recipientName = ClientName.from(message.emitterName),
              message.workflowId,
              message.methodRunId,
              emitterName = emitterName,
          )
          launch { producer.sendToClient(methodRunUnknown) }
        }
        // a workflow wants to dispatch a method on the missing workflow
        if (message.parentWorkflowId != null && message.parentWorkflowId != message.workflowId) {
          val childMethodFailed =
              ChildMethodUnknown(
                  childMethodUnknownError =
                  MethodUnknownError(
                      workflowName = message.workflowName,
                      workflowId = message.workflowId,
                      methodRunId = message.methodRunId,
                  ),
                  workflowName = message.parentWorkflowName ?: thisShouldNotHappen(),
                  workflowId = message.parentWorkflowId!!,
                  methodRunId = message.parentMethodRunId ?: thisShouldNotHappen(),
                  emitterName = emitterName,
              )
          launch { producer.sendToWorkflowEngineLater(childMethodFailed) }
        }
      }

      // a client wants to wait the missing workflow
      is WaitWorkflow -> {
        val methodRunUnknown = MethodRunUnknown(
            recipientName = ClientName.from(message.emitterName),
            message.workflowId,
            message.methodRunId,
            emitterName = emitterName,
        )
        launch { producer.sendToClient(methodRunUnknown) }
      }

      else -> Unit
    }

    logDiscarding(message) { NO_STATE_DISCARDING_REASON }

    return@coroutineScope null
  }

  private suspend fun processMessageWithState(
    message: WorkflowEngineMessage,
    state: WorkflowState
  ): WorkflowState? {

    // if a workflow task is ongoing, we buffer all messages except those associated to a workflowTask
    when (message.isWorkflowTaskEvent()) {
      true -> {
        // Idempotency: discard if this workflowTask is not the current one
        if (state.runningWorkflowTaskId != (message as TaskEvent).taskId()) {
          logDiscarding(message) { "as workflowTask is not the right one" }

          return null
        }
      }

      false -> {
        if (message is WorkflowEvent && state.runningWorkflowTaskId != null) {
          // if a workflow task is ongoing then buffer all WorkflowEvent message,
          // except those associated to a WorkflowTask
          logDebug(message) { "buffering $message" }
          state.messagesBuffer.add(message)

          return state
        }
      }
    }

    coroutineScope {
      // process this message
      processMessage(state, message)

      // process all buffered messages, while there is no workflowTask ongoing
      while (state.runningWorkflowTaskId == null && state.messagesBuffer.size > 0) {
        val msg = state.messagesBuffer.removeAt(0)
        logDebug(msg) { "processing buffered message $msg" }
        processMessage(state, msg)
      }
    }

    return state
  }

  private fun logDiscarding(message: WorkflowEngineMessage, cause: () -> String) {
    val txt = { "Id ${message.workflowId} - discarding ${cause()} $message" }
    when (message) {
      // we don't log these messages as warning as they are expected
      is TaskTimedOut, is ChildMethodTimedOut -> logger.debug(txt)
      else -> logger.warn(txt)
    }
  }

  private fun logDebug(message: WorkflowEngineMessage, txt: () -> String) {
    logger.debug { "Id ${message.workflowId} - ${txt()}" }
  }

  private fun CoroutineScope.processMessage(state: WorkflowState, message: WorkflowEngineMessage) {
    // if message is related to a workflowTask, it's not running anymore
    if (message.isWorkflowTaskEvent()) state.runningWorkflowTaskId = null

    when (message) {
      is DispatchMethodOnRunningWorkflow -> {
        // Idempotency: do not relaunch if this method has already been launched
        if (state.getMethodRun(message.methodRunId) != null) {
          logDiscarding(message) { "as this method has already been launched" }

          return
        }
      }

      is SendSignal -> {
        // Idempotency: do not relaunch if this signal has already been received
        if (state.hasSignalAlreadyBeenReceived(message.signalId)) {
          logDiscarding(message) { "as this signal has already been received" }

          return
        }

      }

      is MethodEvent -> {
        // if methodRun has already been cleaned (completed), then discard the message
        if (state.getMethodRun(message.methodRunId) == null) {
          logDiscarding(message) { "as null methodRun" }

          return
        }
      }

      else -> Unit
    }

    when (message) {
      is DispatchNewWorkflow -> logDiscarding(message) { "as workflow has already started" }
      is DispatchMethodOnRunningWorkflow -> dispatchMethod(producer, state, message)
      is CancelWorkflow -> cancelWorkflow(producer, state, message)
      is SendSignal -> sendSignal(producer, state, message)
      is WaitWorkflow -> waitWorkflow(producer, state, message)
      is CompleteTimers -> completeTimer(state, message)
      is CompleteWorkflow -> TODO() // completeWorkflow(producer, state, message)
      is RetryWorkflowTask -> retryWorkflowTask(producer, state)
      is RetryTasks -> retryTasks(producer, state, message)
      is TimerCompleted -> timerCompleted(producer, state, message)
      is ChildMethodUnknown -> childMethodUnknown(producer, state, message)
      is ChildMethodCanceled -> childMethodCanceled(producer, state, message)
      is ChildMethodTimedOut -> childMethodTimedOut(producer, state, message)
      is ChildMethodFailed -> childMethodFailed(producer, state, message)
      is ChildMethodCompleted -> childMethodCompleted(producer, state, message)

      is TaskCanceled -> when (message.isWorkflowTaskEvent()) {
        true -> TODO() // workflowTaskCanceled(producer, state, message)
        false -> taskCanceled(producer, state, message)
      }

      is TaskTimedOut -> when (message.isWorkflowTaskEvent()) {
        true -> TODO() // workflowTaskTimedOut(producer, state, message)
        false -> taskTimedOut(producer, state, message)
      }

      is TaskFailed -> when (message.isWorkflowTaskEvent()) {
        true -> workflowTaskFailed(producer, state, message)
        false -> taskFailed(producer, state, message)
      }

      is TaskCompleted -> when (message.isWorkflowTaskEvent()) {
        true -> workflowTaskCompleted(producer, state, message)
        false -> taskCompleted(producer, state, message)
      }
    }
  }
}
