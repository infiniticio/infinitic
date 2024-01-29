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
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.DelayedWorkflowEngineTopic
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.transport.WorkflowCmdTopic
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.transport.WorkflowTagTopic
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.tags.messages.AddTagToWorkflow
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import io.infinitic.common.workflows.tags.messages.CompleteTimersByTag
import io.infinitic.common.workflows.tags.messages.DispatchMethodByTag
import io.infinitic.common.workflows.tags.messages.DispatchWorkflowByCustomId
import io.infinitic.common.workflows.tags.messages.GetWorkflowIdsByTag
import io.infinitic.common.workflows.tags.messages.RemoveTagFromWorkflow
import io.infinitic.common.workflows.tags.messages.RetryTasksByTag
import io.infinitic.common.workflows.tags.messages.RetryWorkflowTaskByTag
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import io.infinitic.workflows.tag.storage.LoggedWorkflowTagStorage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class WorkflowTagEngine(
  storage: WorkflowTagStorage,
  producerAsync: InfiniticProducerAsync
) {
  private val storage = LoggedWorkflowTagStorage(this::class.java.name, storage)

  private val producer = LoggedInfiniticProducer(this::class.java.name, producerAsync)

  private val logger = KotlinLogging.logger(this::class.java.name)

  private val emitterName by lazy { EmitterName(producer.name) }

  suspend fun handle(message: WorkflowTagMessage, publishTime: MillisInstant) {
    logger.debug { "receiving $message" }

    when (message) {
      is AddTagToWorkflow -> addTagToWorkflow(message)
      is RemoveTagFromWorkflow -> removeTagFromWorkflow(message)
      is GetWorkflowIdsByTag -> getWorkflowIds(message)
      is DispatchWorkflowByCustomId -> dispatchWorkflowByCustomId(message, publishTime)
      is DispatchMethodByTag -> dispatchMethodByTag(message, publishTime)
      is SendSignalByTag -> sendSignalByTag(message, publishTime)
      is CancelWorkflowByTag -> cancelWorkflowByTag(message, publishTime)
      is RetryWorkflowTaskByTag -> retryWorkflowTaskByTag(message, publishTime)
      is RetryTasksByTag -> retryTaskByTag(message, publishTime)
      is CompleteTimersByTag -> completeTimerByTag(message, publishTime)
    }
  }

  private suspend fun dispatchWorkflowByCustomId(
    message: DispatchWorkflowByCustomId,
    publishTime: MillisInstant
  ) = coroutineScope {
    val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)
    val requester = message.requester

    when (ids.size) {
      // this workflow instance does not exist yet
      0 -> {
        // provided tags
        message.workflowTags.forEach { tag ->
          val addTagToWorkflow = AddTagToWorkflow(
              workflowName = message.workflowName,
              workflowTag = tag,
              workflowId = message.workflowId,
              emitterName = emitterName,
              emittedAt = message.emittedAt ?: publishTime,
          )

          when (tag) {
            message.workflowTag -> addTagToWorkflow(addTagToWorkflow)
            else -> launch { with(producer) { addTagToWorkflow.sendTo(WorkflowTagTopic) } }
          }
        }
        // dispatch workflow message
        val dispatchWorkflow = DispatchWorkflow(
            workflowName = message.workflowName,
            workflowId = message.workflowId,
            methodName = message.methodName,
            methodParameters = message.methodParameters,
            methodParameterTypes = message.methodParameterTypes,
            workflowTags = message.workflowTags,
            workflowMeta = message.workflowMeta,
            requester = requester,
            clientWaiting = message.clientWaiting,
            emitterName = message.emitterName,
            emittedAt = message.emittedAt ?: publishTime,
        )
        launch {
          with(producer) { dispatchWorkflow.sendTo(WorkflowCmdTopic) }
        }

        if (requester is WorkflowRequester) {
          // Sending event message
          launch {
            val childMethodDispatchedEvent =
                dispatchWorkflow.childMethodDispatchedEvent(emitterName)
            with(producer) { childMethodDispatchedEvent.sendTo(WorkflowEventsTopic) }
          }

          // send global timeout if needed
          message.methodTimeout?.let {
            launch {
              val childMethodTimedOut = dispatchWorkflow.childMethodTimedOut(emitterName, it)
              with(producer) { childMethodTimedOut.sendTo(DelayedWorkflowEngineTopic, it) }
            }
          }
        }

      }
      // Another running workflow instance exist with same custom id
      1 -> {
        logger.debug {
          "A workflow '${message.workflowName}(${ids.first()})' already exists with tag '${message.workflowTag}'"
        }

        // if needed, we inform workflowEngine that a client is waiting for its result
        if (message.clientWaiting) {
          launch {
            val waitWorkflow = WaitWorkflow(
                workflowMethodId = WorkflowMethodId.from(ids.first()),
                workflowName = message.workflowName,
                workflowId = ids.first(),
                emitterName = message.emitterName,
                emittedAt = message.emittedAt ?: publishTime,
                requester = requester,
            )
            with(producer) { waitWorkflow.sendTo(WorkflowCmdTopic) }
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

  private suspend fun dispatchMethodByTag(
    message: DispatchMethodByTag,
    publishTime: MillisInstant
  ) = coroutineScope {
    val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)
    val requester = message.requester

    when (ids.isEmpty()) {
      true -> discardTagWithoutIds(message)

      false -> ids.forEach { workflowId ->
        val dispatchMethod = DispatchMethod(
            workflowName = message.workflowName,
            workflowId = workflowId,
            workflowMethodId = message.workflowMethodId,
            methodName = message.methodName,
            methodParameters = message.methodParameters,
            methodParameterTypes = message.methodParameterTypes,
            requester = requester,
            clientWaiting = false,
            emitterName = emitterName,
            emittedAt = message.emittedAt ?: publishTime,
        )

        // parent workflow already applied method to self
        if (requester !is WorkflowRequester || workflowId != requester.workflowId) {
          launch { with(producer) { dispatchMethod.sendTo(WorkflowCmdTopic) } }
        }

        if (requester is WorkflowRequester) {
          // event for the dispatching of a child method
          launch {
            val childMethodDispatchedEvent =
                dispatchMethod.childMethodDispatchedEvent(emitterName)
            with(producer) { childMethodDispatchedEvent.sendTo(WorkflowEventsTopic) }
          }

          // set timeout for the dispatched child method,
          message.methodTimeout?.let {
            launch {
              val childMethodTimedOut = dispatchMethod.childMethodTimedOut(emitterName, it)
              with(producer) { childMethodTimedOut.sendTo(DelayedWorkflowEngineTopic, it) }
            }
          }
        }
      }
    }
  }

  private suspend fun retryWorkflowTaskByTag(
    message: RetryWorkflowTaskByTag,
    publishTime: MillisInstant
  ) = coroutineScope {
    val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)

    when (ids.isEmpty()) {
      true -> discardTagWithoutIds(message)

      false -> ids.forEach { workflowId ->
        launch {
          val retryWorkflowTask = RetryWorkflowTask(
              workflowName = message.workflowName,
              workflowId = workflowId,
              emitterName = emitterName,
              emittedAt = message.emittedAt ?: publishTime,
              requester = message.requester,
          )
          with(producer) { retryWorkflowTask.sendTo(WorkflowCmdTopic) }
        }
      }
    }
  }

  private suspend fun retryTaskByTag(message: RetryTasksByTag, publishTime: MillisInstant) =
      coroutineScope {
        val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)

        when (ids.isEmpty()) {
          true -> discardTagWithoutIds(message)

          false -> ids.forEach { workflowId ->
            launch {
              val retryTasks = RetryTasks(
                  taskId = message.taskId,
                  taskStatus = message.taskStatus,
                  serviceName = message.serviceName,
                  workflowName = message.workflowName,
                  workflowId = workflowId,
                  emitterName = emitterName,
                  emittedAt = message.emittedAt ?: publishTime,
                  requester = message.requester,
              )
              with(producer) { retryTasks.sendTo(WorkflowCmdTopic) }
            }
          }
        }
      }

  private suspend fun completeTimerByTag(message: CompleteTimersByTag, publishTime: MillisInstant) =
      coroutineScope {
        val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)

        when (ids.isEmpty()) {
          true -> discardTagWithoutIds(message)

          false -> ids.forEach { workflowId ->
            launch {
              val completeTimers = CompleteTimers(
                  workflowMethodId = message.workflowMethodId,
                  workflowName = message.workflowName,
                  workflowId = workflowId,
                  emitterName = emitterName,
                  emittedAt = message.emittedAt ?: publishTime,
                  requester = message.requester,
              )
              with(producer) { completeTimers.sendTo(WorkflowCmdTopic) }
            }
          }
        }
      }

  private suspend fun cancelWorkflowByTag(
    message: CancelWorkflowByTag,
    publishTime: MillisInstant
  ) = coroutineScope {
    val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)

    when (ids.isEmpty()) {
      true -> discardTagWithoutIds(message)

      false -> ids.forEach { workflowId ->
        // parent workflow already applied method to self
        if (workflowId != message.emitterWorkflowId) {
          launch {
            val cancelWorkflow = CancelWorkflow(
                cancellationReason = message.reason,
                workflowMethodId = WorkflowMethodId.from(workflowId),
                workflowName = message.workflowName,
                workflowId = workflowId,
                emitterName = emitterName,
                emittedAt = message.emittedAt ?: publishTime,
                requester = message.requester,
            )
            with(producer) { cancelWorkflow.sendTo(WorkflowCmdTopic) }
          }
        }
      }
    }
  }

  private suspend fun sendSignalByTag(message: SendSignalByTag, publishTime: MillisInstant) =
      coroutineScope {
        val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)

        when (ids.isEmpty()) {
          true -> discardTagWithoutIds(message)

          false -> ids.forEach { workflowId ->
            // parent workflow already applied this to itself
            if (workflowId != message.parentWorkflowId) {
              launch {
                val sendSignal = SendSignal(
                    channelName = message.channelName,
                    signalId = message.signalId,
                    signalData = message.signalData,
                    channelTypes = message.channelTypes,
                    workflowName = message.workflowName,
                    workflowId = workflowId,
                    emitterName = emitterName,
                    emittedAt = message.emittedAt ?: publishTime,
                    requester = message.requester,
                )
                with(producer) { sendSignal.sendTo(WorkflowCmdTopic) }
              }
            }
          }
        }
      }

  private suspend fun addTagToWorkflow(message: AddTagToWorkflow) {
    storage.addWorkflowId(message.workflowTag, message.workflowName, message.workflowId)
  }

  private suspend fun removeTagFromWorkflow(message: RemoveTagFromWorkflow) {
    storage.removeWorkflowId(message.workflowTag, message.workflowName, message.workflowId)
  }

  private suspend fun getWorkflowIds(message: GetWorkflowIdsByTag) {
    val workflowIds = storage.getWorkflowIds(message.workflowTag, message.workflowName)

    val workflowIdsByTag = WorkflowIdsByTag(
        recipientName = ClientName.from(message.emitterName),
        message.workflowName,
        message.workflowTag,
        workflowIds,
        emitterName = emitterName,
    )
    with(producer) { workflowIdsByTag.sendTo(ClientTopic) }
  }

  private fun discardTagWithoutIds(message: WorkflowTagMessage) {
    logger.debug { "discarding as no id found for the provided tag: $message" }
  }
}
