/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of the rights granted to you
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
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.requester.ClientRequester
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.executors.errors.MethodUnknownError
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.logged.LoggerWithCounter
import io.infinitic.common.transport.logged.formatLog
import io.infinitic.common.transport.producers.BufferedInfiniticProducer
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.CompleteWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.MethodEvent
import io.infinitic.common.workflows.engine.messages.RemoteMethodCanceled
import io.infinitic.common.workflows.engine.messages.RemoteMethodCompleted
import io.infinitic.common.workflows.engine.messages.RemoteMethodFailed
import io.infinitic.common.workflows.engine.messages.RemoteMethodTimedOut
import io.infinitic.common.workflows.engine.messages.RemoteMethodUnknown
import io.infinitic.common.workflows.engine.messages.RemoteTaskCanceled
import io.infinitic.common.workflows.engine.messages.RemoteTaskCompleted
import io.infinitic.common.workflows.engine.messages.RemoteTaskEvent
import io.infinitic.common.workflows.engine.messages.RemoteTaskFailed
import io.infinitic.common.workflows.engine.messages.RemoteTaskTimedOut
import io.infinitic.common.workflows.engine.messages.RemoteTimerCompleted
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowCompletedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEvent
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class WorkflowStateEngine(
  val storage: WorkflowStateStorage,
  private val _producer: InfiniticProducer
) {

  companion object {
    const val DISPATCH_WORKFLOW_META_DATA = "DISPATCH_WORKFLOW_META_DATA"

    const val UNDEFINED_DUE_TO_RACE_CONDITION = "UNDEFINED_DUE_TO_RACE_CONDITION"

    const val NO_STATE_DISCARDING_REASON = "for having null workflow state"

    val logger = LoggerWithCounter(KotlinLogging.logger {})
  }

  private val emitterName = _producer.emitterName

  suspend fun batchProcess(
    messages: List<Pair<WorkflowStateEngineMessage, MillisInstant>>,
  ) {
    val messagesMap: Map<WorkflowId, List<Pair<WorkflowStateEngineMessage, MillisInstant>>> =
        messages.groupBy { it.first.workflowId }

    // Keep track of remaining messages to process
    var remainingMessages = messagesMap

    // Optimistic locking implementation
    while (remainingMessages.isNotEmpty()) {
      // get current states and versions in a single call
      val currentStatesAndVersions = storage.getStatesAndVersions(remainingMessages.keys.toList())

      // process all messages by workflowId, in parallel
      val producersAndStates = coroutineScope {
        remainingMessages
            .mapValues { (workflowId, messages) ->
              async {
                batchProcessByWorkflowId(
                    currentStatesAndVersions[workflowId]?.first,
                    messages,
                )
              }
            }
            .mapValues { it.value.await() }
      }

      // Send all messages
      coroutineScope {
        producersAndStates.values.forEach { (producer, _) -> launch { producer.send() } }
      }

      // prepare updates with versions
      val updates = producersAndStates.mapValues { (workflowId, producerAndState) ->
        Pair(
            producerAndState.second,
            currentStatesAndVersions[workflowId]?.second ?: 0L,
        )
      }

      // atomically store all states with optimistic locking
      val success = storage.putStatesWithVersions(updates)

      // Keep only the failed updates for retry
      remainingMessages = remainingMessages.filterKeys { workflowId ->
        !(success[workflowId] ?: false)
      }

      // If there are failures, log them
      if (remainingMessages.isNotEmpty()) {
        logger.warn { "Retrying ${remainingMessages.size} failed updates due to version mismatch" }
      }
    }
  }

  private suspend fun batchProcessByWorkflowId(
    currentState: WorkflowState?,
    messages: List<Pair<WorkflowStateEngineMessage, MillisInstant>>
  ): Pair<BufferedInfiniticProducer, WorkflowState?> {
    // do not send messages but buffer them
    val bufferedProducer = BufferedInfiniticProducer(_producer)

    var state: WorkflowState? = currentState
    // process all received messages, starting by the oldest
    messages
        .sortedBy { it.second.long }
        .forEach { (message, publishTime) ->
          state = processSingle(bufferedProducer, state, message, publishTime)
        }

    return Pair(bufferedProducer, state)
  }

  suspend fun process(message: WorkflowStateEngineMessage, publishTime: MillisInstant) {
    // create a producer that buffers messages
    val bufferedProducer = BufferedInfiniticProducer(_producer)

    // Optimistic locking implementation
    while (true) {
      // get current state and version in a single call
      val (state, version) = storage.getStateAndVersion(message.workflowId)

      // process this message
      val updatedState = processSingle(bufferedProducer, state, message, publishTime)

      // send new messages
      bufferedProducer.send()

      // store updated state atomically with optimistic locking
      val success = storage.putStateWithVersion(
          message.workflowId,
          updatedState,
          version, // version is 0 if state was null
      )

      if (success) break
      else logger.warn { "Retrying failed update due to version mismatch" }
      // If update failed, continue to retry
    }
  }

  private suspend fun processSingle(
    producer: InfiniticProducer,
    initialState: WorkflowState?,
    message: WorkflowStateEngineMessage,
    publishTime: MillisInstant
  ): WorkflowState? {

    // if a crash happened between the state update and the message acknowledgement,
    // it's possible to receive a message that has already been processed
    // in this case, we discard it
    if (initialState?.lastMessageId == message.messageId) {
      logDiscarding(message) { "as state already contains messageId ${message.messageId}" }

      return initialState
    }

    // This is needed for compatibility with messages before v0.13.0
    if (message.emittedAt == null) message.emittedAt = publishTime

    // Handling a sneaky race condition that may occur when workers are shutdown and restarted:
    // Due to a Pulsar bug in management of key-shared subscriptions,
    // we may receive the result of a workflow task (RemoteTaskCompleted) or even the results
    // of the actions of the workflow task before receiving the initial DispatchWorkflow
    // that creates the workflow state.

    val raceConditionWithNoState =
        (initialState == null && message !is DispatchWorkflow && message is MethodEvent)
    val raceConditionWithState = (initialState != null &&
        initialState.runningWorkflowTaskId == TaskId(UNDEFINED_DUE_TO_RACE_CONDITION))
    val isWorkflowTaskCompleted = message.isWorkflowTaskEvent() && (message is RemoteTaskCompleted)

    // Specific scenario where an event is received from a workflow,
    // but there is no existing state for that workflow. In such cases,
    // the event is stored in a transient state to be processed later.
    if (raceConditionWithNoState && !isWorkflowTaskCompleted) return WorkflowState(
        lastMessageId = message.messageId,
        workflowId = message.workflowId,
        workflowName = message.workflowName,
        workflowVersion = null,
        workflowTags = setOf(),
        workflowMeta = WorkflowMeta(),
        runningWorkflowTaskId = TaskId(UNDEFINED_DUE_TO_RACE_CONDITION),
        workflowMethods = mutableListOf(),
        messagesBuffer = mutableListOf(message),
    )

    // Specific scenario where an event is received from a workflow,
    // but there is a transient state. In such cases,
    // the event is stored in the transient state to be processed later.
    if (raceConditionWithState && !isWorkflowTaskCompleted)
      return initialState.also { it!!.messagesBuffer.add(message) }

    // Specific scenario where we receive a DispatchWorkflow message
    // while there is a transient state storing a message,
    // then we refine the state and keep the message.
    // (We do not process the event, that should be processed only after a WorkflowTaskCompleted)
    if (raceConditionWithState && message is DispatchWorkflow) {
      return message.newState().copy(messagesBuffer = initialState!!.messagesBuffer)
    }

    val state = when {
      // Specific scenario where we receive the result of a WorkflowTask without an existing state
      // or with a transient state. This should necessarily be the first workflow task, for which
      // the dispatchWorkflow details have been added to the metadata by the WorkflowStateCmdHandler.
      (raceConditionWithNoState || raceConditionWithState) && isWorkflowTaskCompleted -> {
        (message as RemoteTaskCompleted).taskReturnValue.taskMeta[DISPATCH_WORKFLOW_META_DATA]?.let {
          val dispatchWorkflow = AvroSerDe
              .readBinaryWithSchemaFingerprint(it, WorkflowEngineEnvelope::class)
              .message() as DispatchWorkflow
          dispatchWorkflow.newState()
              .copy(messagesBuffer = initialState?.messagesBuffer ?: mutableListOf())
        } ?: initialState
      }

      else -> initialState
    }

    // A transient state should not reach this point
    if (state?.runningWorkflowTaskId == TaskId(UNDEFINED_DUE_TO_RACE_CONDITION)) {
      logger.warn { "This should not happen:\n  state = $initialState \n  msg   = $message" }
    }

    // end of the race condition management

    val updatedState = when (state) {
      null -> processMessageWithoutState(producer, message)
      else -> state.also { processMessageWithState(producer, message, state) }
    }

    return when (updatedState?.workflowMethods?.size) {
      // no state
      null -> null
      // workflow is completed
      0 -> {
        // remove reference to this workflow in tags
        removeTags(producer, updatedState)
        // delete state
        null
      }
      // workflow is ongoing
      else -> {
        // record this message as the last processed
        updatedState.lastMessageId = message.messageId
        // update state
        updatedState
      }
    }
  }

  private suspend fun sendWorkflowCompletedEvent(
    producer: InfiniticProducer,
    state: WorkflowState
  ) {
    val workflowCompletedEvent = WorkflowCompletedEvent(
        workflowName = state.workflowName,
        workflowVersion = state.workflowVersion,
        workflowId = state.workflowId,
        emitterName = emitterName,
    )
    with(producer) { workflowCompletedEvent.sendTo(WorkflowStateEventTopic) }
  }

  private suspend fun removeTags(producer: InfiniticProducer, state: WorkflowState) =
      coroutineScope {
        state.workflowTags.map {
          val removeTagFromWorkflow = RemoveTagFromWorkflow(
              workflowName = state.workflowName,
              workflowTag = it,
              workflowId = state.workflowId,
              emitterName = emitterName,
              emittedAt = state.runningWorkflowTaskInstant,
          )
          launch { with(producer) { removeTagFromWorkflow.sendTo(WorkflowTagEngineTopic) } }
        }
      }

  private suspend fun processMessageWithoutState(
    producer: InfiniticProducer,
    message: WorkflowStateEngineMessage
  ): WorkflowState? = coroutineScope {
    // targeted workflow is not found, we tell the message emitter
    when (message) {
      // New workflow to dispatch
      is DispatchWorkflow -> {
        // Before 0.13, all messages had a null version
        // Since 0.13, the workflow-task is dispatched in workflowCmdHandler
        if (message.infiniticVersion == null) {
          return@coroutineScope dispatchWorkflow(producer, message)
        }

        // all actions are done in WorkflowCmdHandler::dispatchNewWorkflow
        return@coroutineScope message.newState()
      }

      // someone wants to dispatch a method on a workflow unknown or already terminated
      is DispatchMethod -> when (val requester = message.requester ?: thisShouldNotHappen()) {
        is ClientRequester -> {
          if (message.clientWaiting) launch {
            val methodUnknown = MethodUnknown(
                recipientName = ClientName.from(message.emitterName),
                message.workflowId,
                message.workflowMethodId,
                emitterName = emitterName,
            )
            with(producer) { methodUnknown.sendTo(ClientTopic) }
          }
        }

        is WorkflowRequester -> {
          if (requester.workflowId != message.workflowId) launch {
            val childMethodFailed = RemoteMethodUnknown(
                childMethodUnknownError =
                    MethodUnknownError(
                        workflowName = message.workflowName,
                        workflowId = message.workflowId,
                        workflowMethodName = message.workflowMethodName,
                        workflowMethodId = message.workflowMethodId,
                    ),
                workflowName = requester.workflowName,
                workflowId = requester.workflowId,
                workflowVersion = requester.workflowVersion,
                workflowMethodName = requester.workflowMethodName,
                workflowMethodId = requester.workflowMethodId,
                emitterName = emitterName,
                emittedAt = message.emittedAt,
            )
            with(producer) { childMethodFailed.sendTo(WorkflowStateEngineTopic) }
          }
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
    producer: InfiniticProducer,
    message: WorkflowStateEngineMessage,
    state: WorkflowState
  ) {

    // if a workflow task is ongoing, we buffer all messages except those associated to a workflowTask
    when (message.isWorkflowTaskEvent()) {
      true -> {
        // messages had a null version before 0.13
        // we retry workflow task, as commands are not executed anymore
        // inside the engine for version >= 0.13
        if (message.infiniticVersion == null) {
          logDiscarding(message) { "workflowTask that has a null version - retrying it" }
          coroutineScope { retryWorkflowTask(producer, state) }

          return
        }

        // Idempotency: discard if this workflowTask is not the current one
        if (state.runningWorkflowTaskId != TaskId(UNDEFINED_DUE_TO_RACE_CONDITION) &&
          state.runningWorkflowTaskId != (message as RemoteTaskEvent).taskId()) {
          logDiscarding(message) { "as workflowTask ${message.taskId()} is different than ${state.runningWorkflowTaskId} in state" }

          return
        }
      }

      false -> {
        if (message is WorkflowEvent && state.runningWorkflowTaskId != null) {
          // if a workflow task is ongoing then buffer all WorkflowEvent message,
          // except those associated to a WorkflowTask
          logDebug("Buffering:", message)
          state.messagesBuffer.add(message)

          return
        }
      }
    }

    coroutineScope {
      // process this message
      processMessage(producer, state, message)

      // process all buffered messages, while there is no workflowTask ongoing
      while (state.runningWorkflowTaskId == null && state.messagesBuffer.size > 0) {
        val msg = state.messagesBuffer.removeAt(0)
        logDebug("Processing buffered message:", msg)
        processMessage(producer, state, msg)
      }
    }

    // if we just handled a workflow task, and that there is no other workflow task ongoing,
    if (message.isWorkflowTaskEvent() && state.runningWorkflowTaskId == null) {
      // if no method left, then the workflow is completed
      if (state.workflowMethods.isEmpty()) sendWorkflowCompletedEvent(producer, state)
    }
  }

  private fun logDiscarding(message: WorkflowStateEngineMessage, cause: () -> String) {
    val txt = { formatLog(message.workflowId, "Discarding ${cause()}:", message) }
    when (message) {
      // these messages are expected, so we don't log them as warning
      is RemoteTaskTimedOut, is RemoteMethodTimedOut -> logger.debug(txt)
      // this can happen in normal operation
      is RemoteTimerCompleted -> logger.info(txt)
      else -> logger.warn(txt)
    }
  }

  private fun logDebug(command: String, message: WorkflowStateEngineMessage) {
    logger.debug { formatLog(message.workflowId, command, message) }
  }

  private fun CoroutineScope.processMessage(
    producer: InfiniticProducer,
    state: WorkflowState,
    message: WorkflowStateEngineMessage
  ) {
    when (message) {
      is DispatchMethod -> {
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

      is MethodEvent -> {
        // if methodRun has already been cleaned (completed), then discard the message
        if (state.getWorkflowMethod(message.workflowMethodId) == null) {
          logDiscarding(message) { "as there is no running workflow method related" }

          return
        }
      }

      else -> Unit
    }

    // if message is related to a workflowTask, it's not running anymore
    if (message.isWorkflowTaskEvent()) state.runningWorkflowTaskId = null

    when (message) {
      // CMD
      is DispatchWorkflow -> logDiscarding(message) { "as workflow has already started (state: $state)" }
      is DispatchMethod -> dispatchMethod(producer, state, message)
      is CancelWorkflow -> cancelWorkflow(producer, state, message)
      is SendSignal -> sendSignal(producer, state, message)
      is WaitWorkflow -> waitWorkflow(producer, state, message)
      is CompleteTimers -> completeTimer(state, message)
      is CompleteWorkflow -> TODO() // completeWorkflow(producer, state, message)
      is RetryWorkflowTask -> retryWorkflowTask(producer, state)
      is RetryTasks -> retryTasks(producer, state, message)
      // INTERNAL EVENTS
      is RemoteTimerCompleted -> timerCompleted(producer, state, message)
      is RemoteMethodUnknown -> childMethodUnknown(producer, state, message)
      is RemoteMethodCanceled -> childMethodCanceled(producer, state, message)
      is RemoteMethodTimedOut -> childMethodTimedOut(producer, state, message)
      is RemoteMethodFailed -> childMethodFailed(producer, state, message)
      is RemoteMethodCompleted -> childMethodCompleted(producer, state, message)

      is RemoteTaskCanceled -> when (message.isWorkflowTaskEvent()) {
        true -> TODO() // workflowTaskCanceled(producer, state, message)
        false -> taskCanceled(producer, state, message)
      }

      is RemoteTaskTimedOut -> when (message.isWorkflowTaskEvent()) {
        true -> TODO() // workflowTaskTimedOut(producer, state, message)
        false -> taskTimedOut(producer, state, message)
      }

      is RemoteTaskFailed -> when (message.isWorkflowTaskEvent()) {
        true -> workflowTaskFailed(producer, state, message)
        false -> taskFailed(producer, state, message)
      }

      is RemoteTaskCompleted -> when (message.isWorkflowTaskEvent()) {
        true -> workflowTaskCompleted(producer, state, message)
        false -> taskCompleted(producer, state, message)
      }
    }
  }
}
