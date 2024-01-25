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
import io.infinitic.common.clients.messages.MethodUnknown
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.executors.errors.MethodUnknownError
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.transport.WorkflowEngineTopic
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.transport.WorkflowTagTopic
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.ChildMethodCanceled
import io.infinitic.common.workflows.engine.messages.ChildMethodCompleted
import io.infinitic.common.workflows.engine.messages.ChildMethodFailed
import io.infinitic.common.workflows.engine.messages.ChildMethodTimedOut
import io.infinitic.common.workflows.engine.messages.ChildMethodUnknown
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.CompleteWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchMethodWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchNewWorkflow
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.TaskCanceled
import io.infinitic.common.workflows.engine.messages.TaskCompleted
import io.infinitic.common.workflows.engine.messages.TaskFailed
import io.infinitic.common.workflows.engine.messages.TaskTimedOut
import io.infinitic.common.workflows.engine.messages.TimerCompleted
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowCompletedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowInternalEvent
import io.infinitic.common.workflows.engine.messages.WorkflowInternalMethodEvent
import io.infinitic.common.workflows.engine.messages.WorkflowInternalMethodTaskEvent
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.common.workflows.tags.messages.RemoveTagFromWorkflow
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

  private val logger = KotlinLogging.logger(this::class.java.name)
  private val storage = LoggedWorkflowStateStorage(this::class.java.name, storage)
  private val producer = LoggedInfiniticProducer(this::class.java.name, producerAsync)
  private val emitterName by lazy { EmitterName(this::class.java.name) }

  suspend fun handle(message: WorkflowEngineMessage, publishTime: MillisInstant) {
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
      logDiscarding(message) { "as state already contains messageId ${message.messageId}" }

      return
    }

    @Deprecated("This should be removed after v0.13.0")
    if (message.emittedAt == null) message.emittedAt = publishTime

    state = when (state) {
      null -> processMessageWithoutState(message)
      else -> processMessageWithState(message, state)
    } ?: return

    when (state.workflowMethods.size) {
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

  private suspend fun sendWorkflowCompletedEvent(state: WorkflowState) {
    val workflowCompletedEvent = WorkflowCompletedEvent(
        workflowName = state.workflowName,
        workflowId = state.workflowId,
        workflowMeta = state.workflowMeta,
        workflowTags = state.workflowTags,
        emitterName = emitterName,
    )
    with(producer) { workflowCompletedEvent.sendTo(WorkflowEventsTopic) }
  }

  private suspend fun removeTags(producer: InfiniticProducer, state: WorkflowState) =
      coroutineScope {
        state.workflowTags.map {
          val removeTagFromWorkflow = RemoveTagFromWorkflow(
              workflowName = state.workflowName,
              workflowTag = it,
              workflowId = state.workflowId,
              emitterName = EmitterName(producer.name),
              emittedAt = state.runningWorkflowTaskInstant,
          )
          launch { with(producer) { removeTagFromWorkflow.sendTo(WorkflowTagTopic) } }
        }
      }

  private suspend fun processMessageWithoutState(
    message: WorkflowEngineMessage
  ): WorkflowState? = coroutineScope {
    // targeted workflow is not found, we tell the message emitter
    when (message) {
      // New workflow to dispatch
      is DispatchNewWorkflow -> {
        // Before 0.13, all messages had a null version
        // Since 0.13, the workflow-task is dispatched in workflowCmdHandler
        @Deprecated("This should be removed after v0.13.0")
        if (message.version == null) {
          return@coroutineScope dispatchWorkflow(producer, message)
        }

        // all actions are now done in WorkflowCmdHandler::dispatchNewWorkflow
        return@coroutineScope message.state()
      }

      // a client wants to dispatch a method on an unknown workflow
      is DispatchMethodWorkflow -> {
        if (message.clientWaiting) launch {
          val methodUnknown = MethodUnknown(
              recipientName = ClientName.from(message.emitterName),
              message.workflowId,
              message.workflowMethodId,
              emitterName = emitterName,
          )
          with(producer) { methodUnknown.sendTo(ClientTopic) }
        }
        // a workflow wants to dispatch a method on an unknown workflow
        if (message.requesterWorkflowId != null && message.requesterWorkflowId != message.workflowId) launch {
          val childMethodFailed = ChildMethodUnknown(
              childMethodUnknownError =
              MethodUnknownError(
                  workflowName = message.workflowName,
                  workflowId = message.workflowId,
                  workflowMethodId = message.workflowMethodId,
              ),
              workflowName = message.requesterWorkflowName ?: thisShouldNotHappen(),
              workflowId = message.requesterWorkflowId!!,
              workflowMethodId = message.requesterWorkflowMethodId ?: thisShouldNotHappen(),
              emitterName = emitterName,
              emittedAt = message.emittedAt,
          )
          with(producer) { childMethodFailed.sendTo(WorkflowEngineTopic) }
        }
      }

      // a client wants to wait the missing workflow
      is WaitWorkflow -> launch {
        val methodUnknown = MethodUnknown(
            recipientName = ClientName.from(message.emitterName),
            message.workflowId,
            message.workflowMethodId,
            emitterName = emitterName,
        )
        with(producer) { methodUnknown.sendTo(ClientTopic) }
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
        // messages had a null version before 0.13
        // we retry workflow task, as commands are not executed anymore
        // inside the engine for version >= 0.13
        @Deprecated("This should be removed after v0.13.0")
        if (message.version == null) {
          logDiscarding(message) { "workflowTask that has a null version - retrying it" }
          coroutineScope { retryWorkflowTask(producer, state) }

          return state
        }

        // Idempotency: discard if this workflowTask is not the current one
        if (state.runningWorkflowTaskId != (message as WorkflowInternalMethodTaskEvent).taskId()) {
          logDiscarding(message) { "as workflowTask ${message.taskId()} is different than ${state.runningWorkflowTaskId} in state" }

          return null
        }
      }

      false -> {
        if (message is WorkflowInternalEvent && state.runningWorkflowTaskId != null) {
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

    // if we just handled a workflow task, and that there is no other workflow task ongoing,
    if (message.isWorkflowTaskEvent() && state.runningWorkflowTaskId == null) {
      // if no method left, then the workflow is completed
      if (state.workflowMethods.isEmpty()) sendWorkflowCompletedEvent(state)
    }

    return state
  }

  private fun logDiscarding(message: WorkflowEngineMessage, cause: () -> String) {
    val txt = { "Id ${message.workflowId} - discarding ${cause()}: $message" }
    when (message) {
      // these messages are expected, so we don't log them as warning
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
      is DispatchMethodWorkflow -> {
        // Idempotency: do not relaunch if this method has already been launched
        if (state.getWorkflowMethod(message.workflowMethodId) != null) {
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

      is WorkflowInternalMethodEvent -> {
        // if methodRun has already been cleaned (completed), then discard the message
        if (state.getWorkflowMethod(message.workflowMethodId) == null) {
          logDiscarding(message) { "as null methodRun" }

          return
        }
      }

      else -> Unit
    }

    when (message) {
      // CMD
      is DispatchNewWorkflow -> logDiscarding(message) { "as workflow has already started with state $state" }
      is DispatchMethodWorkflow -> dispatchMethod(producer, state, message)
      is CancelWorkflow -> cancelWorkflow(producer, state, message)
      is SendSignal -> sendSignal(producer, state, message)
      is WaitWorkflow -> waitWorkflow(producer, state, message)
      is CompleteTimers -> completeTimer(state, message)
      is CompleteWorkflow -> TODO() // completeWorkflow(producer, state, message)
      is RetryWorkflowTask -> retryWorkflowTask(producer, state)
      is RetryTasks -> retryTasks(producer, state, message)
      // INTERNAL EVENTS
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
