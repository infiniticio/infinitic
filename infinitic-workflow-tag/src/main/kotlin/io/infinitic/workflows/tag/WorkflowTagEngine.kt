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
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.requester.workflowId
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.logged.LoggerWithCounter
import io.infinitic.common.transport.producers.BufferedInfiniticProducer
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
import io.infinitic.common.workflows.tags.storage.WorkflowIdsPage
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

    val messagesMap: Map<Pair<WorkflowTag, WorkflowName>, List<Pair<WorkflowTagEngineMessage, MillisInstant>>> =
        messages.groupBy { it.first.workflowTag to it.first.workflowName }

    // Keep one mutable view per tag/name group, as in 0.18.2. Most groups only need it to
    // accumulate add/remove deltas; groups with full-set reads get preloaded below.
    val groupsNeedingFullIds = messagesMap.filterValues { groupedMessages ->
      groupedMessages.any { (message, _) -> message.needsFullIdsInBufferedStorage() }
    }.keys

    val setIds = when (groupsNeedingFullIds.isEmpty()) {
      true -> emptyMap()
      false -> storage.getWorkflowIds(groupsNeedingFullIds)
    }
    val bufferedStorages = messagesMap.keys.associateWith {
      BufferedWorkflowTagStorage(setIds[it]?.toMutableSet())
    }
    val bufferedProducers = messagesMap.keys.associateWith {
      BufferedInfiniticProducer(producer)
    }

    // Each tag/name group keeps publish-time order internally; groups run independently.
    coroutineScope {
      messagesMap.map { (tagAndName, groupedMessages) ->
        val bufferedStorage = bufferedStorages[tagAndName]!!
        val bufferedProducer = bufferedProducers[tagAndName]!!
        launch {
          batchProcessByTag(
              bufferedStorage = bufferedStorage,
              storage = storage,
              bufferedProducer = bufferedProducer,
              messages = groupedMessages,
          )
        }
      }
    }

    // Publish only after every group has finished so batch output is emitted as one unit.
    coroutineScope {
      bufferedProducers.values.forEach { launch { it.send() } }
    }

    val adds = bufferedStorages
        .mapValues { it.value.adds }
        .filterValues { it.isNotEmpty() }
    val removes = bufferedStorages
        .mapValues { it.value.removes }
        .filterValues { it.isNotEmpty() }

    // Flush accumulated tag mutations once, preserving the batch-storage optimization.
    if (adds.isNotEmpty() || removes.isNotEmpty()) {
      storage.updateWorkflowIds(
          add = adds,
          remove = removes,
      )
    }
  }

  private suspend fun batchProcessByTag(
    bufferedStorage: BufferedWorkflowTagStorage,
    storage: WorkflowTagStorage,
    bufferedProducer: BufferedInfiniticProducer,
    messages: List<Pair<WorkflowTagEngineMessage, MillisInstant>>
  ) {
    // Shared by this tag/name batch only, so equal fan-outs reuse one page read.
    val pageCache = mutableMapOf<FanoutPageKey, WorkflowIdsPage>()

    messages
        .sortedBy { it.second.long }
        .forEach { (message, publishTime) ->
          process(bufferedStorage, storage, bufferedProducer, message, publishTime, pageCache)
        }
  }

  suspend fun process(message: WorkflowTagEngineMessage, publishTime: MillisInstant) =
      process(storage, storage, producer, message, publishTime)

  private suspend fun process(
    bufferedStorage: WorkflowTagStorage,
    storage: WorkflowTagStorage,
    bufferedProducer: InfiniticProducer,
    message: WorkflowTagEngineMessage,
    publishTime: MillisInstant,
    pageCache: MutableMap<FanoutPageKey, WorkflowIdsPage>? = null,
  ) {
    when (message) {
      // commands that change the storage
      is AddTagToWorkflow -> addTagToWorkflow(bufferedStorage, message)
      is RemoveTagFromWorkflow -> removeTagFromWorkflow(bufferedStorage, message)
      // commands that require all ids
      is GetWorkflowIdsByTag -> getWorkflowIds(bufferedStorage, bufferedProducer, message)
      is DispatchWorkflowByCustomId -> dispatchByCustomId(bufferedStorage, bufferedProducer, message, publishTime)
      // Fan-outs are paged from durable storage, not from the batch buffer. That preserves
      // pagination without loading every id, but same-batch add/remove mutations are not visible.
      is DispatchMethodByTag,
      is SendSignalByTag,
      is CancelWorkflowByTag,
      is RetryWorkflowTaskByTag,
      is RetryTasksByTag,
      is CompleteTimersByTag, -> processInitialFanout(
          storage,
          bufferedProducer,
          message,
          publishTime,
          pageCache,
      )

      is ContinueWorkflowTagFanout -> continueFanout(
          storage,
          bufferedProducer,
          message,
          publishTime,
          pageCache,
      )
    }
  }

  private suspend fun processInitialFanout(
    storage: WorkflowTagStorage,
    producer: InfiniticProducer,
    message: WorkflowTagFanoutMessage,
    publishTime: MillisInstant,
    pageCache: MutableMap<FanoutPageKey, WorkflowIdsPage>?,
  ) {
    val normalizedMessage = normalizeFanoutMessage(message, publishTime)
    // Initial fan-outs process the first page now; continuations start only at the next cursor.
    processFanoutPage(
        storage = storage,
        producer = producer,
        operationId = message.messageId ?: thisShouldNotHappen(),
        limit = fanoutPageSize,
        cursor = null,
        command = normalizedMessage,
        emittedAt = normalizedMessage.emittedAt ?: publishTime,
        pageCache = pageCache,
    )
  }

  private suspend fun continueFanout(
    storage: WorkflowTagStorage,
    producer: InfiniticProducer,
    message: ContinueWorkflowTagFanout,
    publishTime: MillisInstant,
    pageCache: MutableMap<FanoutPageKey, WorkflowIdsPage>?,
  ) {
    val command = normalizeFanoutMessage(message.command(), message.emittedAt ?: publishTime)
    // Existing continuation messages resume at their stored cursor.
    processFanoutPage(
        storage = storage,
        producer = producer,
        operationId = message.operationId,
        limit = message.limit,
        cursor = message.cursor,
        command = command,
        emittedAt = message.emittedAt ?: command.emittedAt ?: publishTime,
        pageCache = pageCache,
    )
  }

  private suspend fun processFanoutPage(
    storage: WorkflowTagStorage,
    producer: InfiniticProducer,
    operationId: MessageId,
    limit: Int,
    cursor: String?,
    command: WorkflowTagFanoutMessage,
    emittedAt: MillisInstant,
    pageCache: MutableMap<FanoutPageKey, WorkflowIdsPage>?,
  ) {
    val workflowIdsPage = getWorkflowIdsPage(storage, command, limit, cursor, pageCache)

    if (workflowIdsPage.workflowIds.isEmpty()) {
      if (cursor == null) discardTagWithoutIds(command as WorkflowTagEngineMessage)
      return
    }

    fanoutPage(producer, command, workflowIdsPage.workflowIds)

    // A continuation is needed only when the storage returned another cursor.
    workflowIdsPage.nextCursor?.let { nextCursor ->
      val continuation = ContinueWorkflowTagFanout.from(
          operationId = operationId,
          limit = limit,
          cursor = nextCursor,
          command = command,
          emitterName = emitterName,
          emittedAt = emittedAt,
      )
      with(producer) { continuation.sendTo(WorkflowTagEngineTopic) }
    }
  }

  private suspend fun getWorkflowIdsPage(
    storage: WorkflowTagStorage,
    command: WorkflowTagFanoutMessage,
    limit: Int,
    cursor: String?,
    pageCache: MutableMap<FanoutPageKey, WorkflowIdsPage>?,
  ): WorkflowIdsPage {
    val key = FanoutPageKey(
        tag = command.workflowTag,
        workflowName = command.workflowName,
        limit = limit,
        cursor = cursor,
    )
    pageCache?.get(key)?.let { return it }

    val page = storage.getWorkflowIdsPage(
        tag = command.workflowTag,
        workflowName = command.workflowName,
        limit = limit,
        cursor = cursor,
    )
    if (pageCache != null) {
      pageCache[key] = page
    }

    return page
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

  // Fan-outs are paged separately; only full-set commands need the batch buffer preloaded.
  private fun WorkflowTagEngineMessage.needsFullIdsInBufferedStorage() = when (this) {
    is DispatchWorkflowByCustomId,
    is GetWorkflowIdsByTag,
    -> true

    else -> false
  }

  companion object {
    val logger = LoggerWithCounter(KotlinLogging.logger {})
  }
}

private data class FanoutPageKey(
  val tag: WorkflowTag,
  val workflowName: WorkflowName,
  val limit: Int,
  val cursor: String?,
)
