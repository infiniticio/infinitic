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
package io.infinitic.workflows.tag

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.WorkflowIdsByTag
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.requester.workflowId
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.logged.LoggerWithCounter
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.commands.dispatchRemoteMethod
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.data.RemoteMethodDispatchedById
import io.infinitic.common.workflows.engine.messages.data.RemoteWorkflowDispatched
import io.infinitic.common.workflows.tags.messages.AddTagToWorkflow
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import io.infinitic.common.workflows.tags.messages.CompleteTimersByTag
import io.infinitic.common.workflows.tags.messages.ContinueWorkflowTagFanout
import io.infinitic.common.workflows.tags.messages.DispatchMethodByTag
import io.infinitic.common.workflows.tags.messages.DispatchWorkflowByCustomId
import io.infinitic.common.workflows.tags.messages.GetWorkflowIdsByTag
import io.infinitic.common.workflows.tags.messages.RemoveTagFromWorkflow
import io.infinitic.common.workflows.tags.messages.RetryTasksByTag
import io.infinitic.common.workflows.tags.messages.RetryWorkflowTaskByTag
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagFanoutMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import io.infinitic.workflows.tag.storage.BufferedWorkflowTagStorage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class WorkflowTagEngine(
  private val storage: WorkflowTagStorage,
  private val producer: InfiniticProducer,
  private val fanoutPageSize: Int = 5000,
) {

  init {
    require(fanoutPageSize > 0) { "fanoutPageSize must be positive" }
  }

  private val emitterName = producer.emitterName

  suspend fun batchProcess(
    messages: List<Pair<WorkflowTagEngineMessage, MillisInstant>>,
  ) {
    if (messages.isEmpty()) return

    if (messages.all { (message, _) -> message is AddTagToWorkflow || message is RemoveTagFromWorkflow }) {
      batchProcessAddRemoveOnly(messages)
      return
    }

    val messagesMap: Map<Pair<WorkflowTag, WorkflowName>, List<Pair<WorkflowTagEngineMessage, MillisInstant>>> =
        messages.groupBy { it.first.workflowTag to it.first.workflowName }

    coroutineScope {
      messagesMap
          .map { (tagAndName, messages) ->
            launch { batchProcessByTag(storage, producer, messages) }
          }
    }
  }

  private suspend fun batchProcessAddRemoveOnly(
    messages: List<Pair<WorkflowTagEngineMessage, MillisInstant>>,
  ) {
    val messagesMap: Map<Pair<WorkflowTag, WorkflowName>, List<Pair<WorkflowTagEngineMessage, MillisInstant>>> =
        messages.groupBy { it.first.workflowTag to it.first.workflowName }

    val setIds = storage.getWorkflowIds(messagesMap.keys)
    val storages = messagesMap.keys.associateWith {
      BufferedWorkflowTagStorage(setIds[it]?.toMutableSet() ?: mutableSetOf())
    }

    coroutineScope {
      messagesMap.map { (tagAndName, groupedMessages) ->
        val bufferedStorage = storages[tagAndName]!!
        launch { batchProcessByTag(bufferedStorage, producer, groupedMessages) }
      }
    }

    storage.updateWorkflowIds(
        add = storages.mapValues { it.value.adds },
        remove = storages.mapValues { it.value.removes },
    )
  }

  private suspend fun batchProcessByTag(
    storage: WorkflowTagStorage,
    producer: InfiniticProducer,
    messages: List<Pair<WorkflowTagEngineMessage, MillisInstant>>
  ) {
    messages
        .sortedBy { it.second.long }
        .forEach { (message, publishTime) ->
          process(storage, producer, message, publishTime)
        }
  }

  suspend fun process(message: WorkflowTagEngineMessage, publishTime: MillisInstant) =
      process(storage, producer, message, publishTime)

  private suspend fun process(
    storage: WorkflowTagStorage,
    producer: InfiniticProducer,
    message: WorkflowTagEngineMessage,
    publishTime: MillisInstant
  ) {
    when (message) {
      is AddTagToWorkflow -> addTagToWorkflow(storage, message)
      is RemoveTagFromWorkflow -> removeTagFromWorkflow(storage, message)
      is GetWorkflowIdsByTag -> getWorkflowIds(storage, producer, message)
      is DispatchWorkflowByCustomId -> dispatchByCustomId(storage, producer, message, publishTime)
      is DispatchMethodByTag -> inlineFanout(storage, producer, message, publishTime)
      is SendSignalByTag -> inlineFanout(storage, producer, message, publishTime)
      is CancelWorkflowByTag -> inlineFanout(storage, producer, message, publishTime)
      is RetryWorkflowTaskByTag -> inlineFanout(storage, producer, message, publishTime)
      is RetryTasksByTag -> inlineFanout(storage, producer, message, publishTime)
      is CompleteTimersByTag -> inlineFanout(storage, producer, message, publishTime)
      is ContinueWorkflowTagFanout -> continueFanout(storage, producer, message)
    }
  }

  private suspend fun inlineFanout(
    storage: WorkflowTagStorage,
    producer: InfiniticProducer,
    message: WorkflowTagFanoutMessage,
    publishTime: MillisInstant,
  ) {
    val normalizedMessage = normalizeFanoutMessage(message, publishTime)

    val page = storage.getWorkflowIdsPage(
        tag = normalizedMessage.workflowTag,
        workflowName = normalizedMessage.workflowName,
        limit = fanoutPageSize,
    )

    if (page.workflowIds.isEmpty()) {
      discardTagWithoutIds(normalizedMessage as WorkflowTagEngineMessage)
      return
    }

    fanoutPage(producer, normalizedMessage, page.workflowIds)

    // Only use self-messaging for subsequent pages
    page.nextCursor?.let { nextCursor ->
      val continuation = ContinueWorkflowTagFanout.from(
          operationId = message.messageId ?: thisShouldNotHappen(),
          limit = fanoutPageSize,
          cursor = nextCursor,
          command = normalizedMessage,
          emitterName = emitterName,
          emittedAt = normalizedMessage.emittedAt,
      )
      with(producer) { continuation.sendTo(WorkflowTagEngineTopic) }
    }
  }

  private suspend fun continueFanout(
    storage: WorkflowTagStorage,
    producer: InfiniticProducer,
    message: ContinueWorkflowTagFanout,
  ) {
    val command = message.command()
    val workflowIdsPage = storage.getWorkflowIdsPage(
        tag = command.workflowTag,
        workflowName = command.workflowName,
        limit = message.limit,
        cursor = message.cursor,
    )

    if (workflowIdsPage.workflowIds.isEmpty()) {
      if (message.cursor == null) discardTagWithoutIds(command as WorkflowTagEngineMessage)
      return
    }

    fanoutPage(producer, command, workflowIdsPage.workflowIds)

    workflowIdsPage.nextCursor?.let { nextCursor ->
      val continuation = ContinueWorkflowTagFanout.from(
          operationId = message.operationId,
          limit = message.limit,
          cursor = nextCursor,
          command = command,
          emitterName = emitterName,
          emittedAt = message.emittedAt,
      )
      with(producer) { continuation.sendTo(WorkflowTagEngineTopic) }
    }
  }

  private suspend fun dispatchByCustomId(
    storage: WorkflowTagStorage,
    producer: InfiniticProducer,
    message: DispatchWorkflowByCustomId,
    publishTime: MillisInstant
  ) = coroutineScope {
    val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)
    val requester = message.requester ?: thisShouldNotHappen()

    when (ids.size) {
      // this workflow instance does not exist yet
      0 -> {
        val remoteWorkflowDispatched = with(message) {
          RemoteWorkflowDispatched(
              workflowId = workflowId,
              workflowName = workflowName,
              workflowMethodId = WorkflowMethodId.from(workflowId),
              workflowMethodName = methodName,
              methodName = methodName,
              methodParameters = methodParameters,
              methodParameterTypes = methodParameterTypes,
              workflowTags = workflowTags,
              workflowMeta = workflowMeta,
              timeout = methodTimeout,
              emittedAt = emittedAt ?: publishTime,
          )
        }
        with(producer) { dispatchRemoteMethod(remoteWorkflowDispatched, requester) }

        // add customId tag
        val addTagToWorkflow = with(message) {
          AddTagToWorkflow(
              workflowName = workflowName,
              workflowTag = workflowTag,
              workflowId = workflowId,
              emitterName = emitterName,
              emittedAt = emittedAt,
          )
        }
        addTagToWorkflow(storage, addTagToWorkflow)
      }
      // Another running workflow instance already exist with same custom id
      // TODO: the way clientWaiting is used here can be tricky, as nothing guarantees that
      //  this call is similar to the previous one, maybe sending back an error would be more relevant
      1 -> {
        val workflowId = ids.first()

        logger.info {
          "Not launching new `${message.workflowName}` workflow as there is already `$workflowId` with tag `${message.workflowTag}`"
        }

        // if needed, we inform workflowEngine that a client is waiting for its result
        if (message.clientWaiting) {
          launch {
            val waitWorkflow = WaitWorkflow(
                workflowMethodId = WorkflowMethodId.from(workflowId),
                workflowName = message.workflowName,
                workflowId = workflowId,
                emitterName = message.emitterName,
                emittedAt = message.emittedAt ?: publishTime,
                requester = requester,
            )
            with(producer) { waitWorkflow.sendTo(WorkflowStateCmdTopic) }
          }
        }

        Unit
      }
      // multiple running workflow instance exist with same custom id
      else -> thisShouldNotHappen(
          "Workflow '${message.workflowName}' with customId '${message.workflowTag}' has multiple ids: ${ids.joinToString()}",
      )
    }
  }

  private suspend fun fanoutPage(
    producer: InfiniticProducer,
    message: WorkflowTagFanoutMessage,
    workflowIds: List<WorkflowId>,
  ) = coroutineScope {
    when (message) {
      is DispatchMethodByTag -> fanoutDispatchMethodByTag(producer, message, workflowIds)
      is SendSignalByTag -> fanoutSendSignalByTag(producer, message, workflowIds)
      is CancelWorkflowByTag -> fanoutCancelWorkflowByTag(producer, message, workflowIds)
      is RetryWorkflowTaskByTag -> fanoutRetryWorkflowTaskByTag(producer, message, workflowIds)
      is RetryTasksByTag -> fanoutRetryTasksByTag(producer, message, workflowIds)
      is CompleteTimersByTag -> fanoutCompleteTimersByTag(producer, message, workflowIds)
      else -> thisShouldNotHappen()
    }
  }

  private suspend fun fanoutDispatchMethodByTag(
    producer: InfiniticProducer,
    message: DispatchMethodByTag,
    workflowIds: List<WorkflowId>,
  ) = coroutineScope {
    val requester = message.requester ?: thisShouldNotHappen()

    forEachBounded(workflowIds) { workflowId ->
      val remoteMethodDispatchedById = with(message) {
        RemoteMethodDispatchedById(
            workflowId = workflowId,
            workflowName = workflowName,
            workflowMethodId = workflowMethodId,
            workflowMethodName = methodName,
            methodName = methodName,
            methodParameters = methodParameters,
            methodParameterTypes = methodParameterTypes,
            timeout = methodTimeout,
            emittedAt = emittedAt ?: thisShouldNotHappen(),
        )
      }
      with(producer) { dispatchRemoteMethod(remoteMethodDispatchedById, requester) }
    }
  }

  private suspend fun fanoutRetryWorkflowTaskByTag(
    producer: InfiniticProducer,
    message: RetryWorkflowTaskByTag,
    workflowIds: List<WorkflowId>,
  ) = coroutineScope {
    forEachBounded(workflowIds) { workflowId ->
      val retryWorkflowTask = RetryWorkflowTask(
          workflowName = message.workflowName,
          workflowId = workflowId,
          emitterName = emitterName,
          emittedAt = message.emittedAt ?: thisShouldNotHappen(),
          requester = message.requester,
      )
      with(producer) { retryWorkflowTask.sendTo(WorkflowStateCmdTopic) }
    }
  }

  private suspend fun fanoutRetryTasksByTag(
    producer: InfiniticProducer,
    message: RetryTasksByTag,
    workflowIds: List<WorkflowId>,
  ) = coroutineScope {
    forEachBounded(workflowIds) { workflowId ->
      val retryTasks = RetryTasks(
          taskId = message.taskId,
          taskStatus = message.taskStatus,
          serviceName = message.serviceName,
          workflowName = message.workflowName,
          workflowId = workflowId,
          emitterName = emitterName,
          emittedAt = message.emittedAt ?: thisShouldNotHappen(),
          requester = message.requester,
      )
      with(producer) { retryTasks.sendTo(WorkflowStateCmdTopic) }
    }
  }

  private suspend fun fanoutCompleteTimersByTag(
    producer: InfiniticProducer,
    message: CompleteTimersByTag,
    workflowIds: List<WorkflowId>,
  ) = coroutineScope {
    forEachBounded(workflowIds) { workflowId ->
      val completeTimers = CompleteTimers(
          workflowMethodId = message.workflowMethodId,
          workflowName = message.workflowName,
          workflowId = workflowId,
          emitterName = emitterName,
          emittedAt = message.emittedAt ?: thisShouldNotHappen(),
          requester = message.requester,
      )
      with(producer) { completeTimers.sendTo(WorkflowStateCmdTopic) }
    }
  }

  private suspend fun fanoutCancelWorkflowByTag(
    producer: InfiniticProducer,
    message: CancelWorkflowByTag,
    workflowIds: List<WorkflowId>,
  ) = coroutineScope {
    forEachBounded(workflowIds.filter { it != message.requester.workflowId }) { workflowId ->
      val cancelWorkflow = CancelWorkflow(
          cancellationReason = message.reason,
          workflowMethodId = null,
          workflowName = message.workflowName,
          workflowId = workflowId,
          emitterName = emitterName,
          emittedAt = message.emittedAt ?: thisShouldNotHappen(),
          requester = message.requester,
      )
      with(producer) { cancelWorkflow.sendTo(WorkflowStateCmdTopic) }
    }
  }

  private suspend fun fanoutSendSignalByTag(
    producer: InfiniticProducer,
    message: SendSignalByTag,
    workflowIds: List<WorkflowId>,
  ) = coroutineScope {
    val requesterWorkflowId = message.requester.workflowId ?: message.parentWorkflowId

    forEachBounded(workflowIds.filter { it != requesterWorkflowId }) { workflowId ->
      val sendSignal = with(message) {
        SendSignal(
            workflowName = workflowName,
            workflowId = workflowId,
            signalId = signalId,
            signalData = signalData,
            channelName = channelName,
            channelTypes = channelTypes,
            emitterName = emitterName,
            emittedAt = emittedAt ?: thisShouldNotHappen(),
            requester = requester,
        )
      }
      with(producer) { sendSignal.sendTo(WorkflowStateCmdTopic) }
    }
  }

  private suspend fun <T> forEachBounded(
    items: List<T>,
    block: suspend (T) -> Unit,
  ) = coroutineScope {
    items.forEach { item ->
      launch { block(item) }
    }
  }

  private fun normalizeFanoutMessage(
    message: WorkflowTagFanoutMessage,
    publishTime: MillisInstant,
  ): WorkflowTagFanoutMessage = when (message) {
    is DispatchMethodByTag -> message.copy(emittedAt = message.emittedAt ?: publishTime)
    is SendSignalByTag -> message.copy(emittedAt = message.emittedAt ?: publishTime)
    is CancelWorkflowByTag -> message.copy(emittedAt = message.emittedAt ?: publishTime)
    is RetryWorkflowTaskByTag -> message.copy(emittedAt = message.emittedAt ?: publishTime)
    is RetryTasksByTag -> message.copy(emittedAt = message.emittedAt ?: publishTime)
    is CompleteTimersByTag -> message.copy(emittedAt = message.emittedAt ?: publishTime)
    else -> thisShouldNotHappen()
  }

  private suspend fun getWorkflowIds(
    storage: WorkflowTagStorage,
    producer: InfiniticProducer,
    message: GetWorkflowIdsByTag
  ) {
    val workflowIds = storage.getWorkflowIds(message.workflowTag, message.workflowName)

    val workflowIdsByTag = WorkflowIdsByTag(
        recipientName = ClientName.from(message.emitterName),
        workflowName = message.workflowName,
        workflowTag = message.workflowTag,
        workflowIds = workflowIds,
        emitterName = emitterName,
    )
    with(producer) { workflowIdsByTag.sendTo(ClientTopic) }
  }

  private suspend fun addTagToWorkflow(
    storage: WorkflowTagStorage,
    message: AddTagToWorkflow
  ) {
    storage.addWorkflowId(message.workflowTag, message.workflowName, message.workflowId)
  }

  private suspend fun removeTagFromWorkflow(
    storage: WorkflowTagStorage,
    message: RemoveTagFromWorkflow
  ) {
    storage.removeWorkflowId(message.workflowTag, message.workflowName, message.workflowId)
  }

  private fun discardTagWithoutIds(message: WorkflowTagEngineMessage) {
    logger.info { "discarding ${message::class.simpleName} as no workflow `${message.workflowName}` found for tag `${message.workflowTag}`" }
  }

  companion object {
    val logger = LoggerWithCounter(KotlinLogging.logger {})
  }
}
