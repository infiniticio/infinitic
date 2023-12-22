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
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.executors.errors.MethodTimedOutError
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.ChildMethodTimedOut
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.DispatchMethodOnRunningWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchNewWorkflow
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
  private val storage = LoggedWorkflowTagStorage(javaClass.name, storage)

  private val producer = LoggedInfiniticProducer(javaClass.name, producerAsync)

  private val logger = KotlinLogging.logger(javaClass.name)

  private val emitterName by lazy { EmitterName(producer.name) }

  suspend fun handle(message: WorkflowTagMessage) {
    logger.debug { "receiving $message" }

    when (message) {
      is DispatchWorkflowByCustomId -> dispatchWorkflowByCustomId(message)
      is DispatchMethodByTag -> dispatchMethodByTag(message)
      is AddTagToWorkflow -> addWorkflowTag(message)
      is RemoveTagFromWorkflow -> removeWorkflowTag(message)
      is SendSignalByTag -> sendToChannelByTag(message)
      is CancelWorkflowByTag -> cancelWorkflowByTag(message)
      is RetryWorkflowTaskByTag -> retryWorkflowTaskByTag(message)
      is RetryTasksByTag -> retryTaskByTag(message)
      is CompleteTimersByTag -> completeTimerByTag(message)
      is GetWorkflowIdsByTag -> getWorkflowIds(message)
    }
  }

  private suspend fun dispatchWorkflowByCustomId(message: DispatchWorkflowByCustomId) {
    val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)

    // with coroutineScope, we send messages in parallel and wait for all of them to be processed
    coroutineScope {
      when (ids.size) {
        // this workflow instance does not exist yet
        0 -> {
          // provided tags
          message.workflowTags.map {
            val addTagToWorkflow = AddTagToWorkflow(
                workflowName = message.workflowName,
                workflowTag = it,
                workflowId = message.workflowId,
                emitterName = emitterName,
            )

            when (it) {
              message.workflowTag -> addWorkflowTag(addTagToWorkflow)
              else -> launch { producer.sendToWorkflowTag(addTagToWorkflow) }
            }
          }
          // dispatch workflow message
          val dispatchWorkflow = DispatchNewWorkflow(
              workflowName = message.workflowName,
              workflowId = message.workflowId,
              methodName = message.methodName,
              methodParameters = message.methodParameters,
              methodParameterTypes = message.methodParameterTypes,
              workflowTags = message.workflowTags,
              workflowMeta = message.workflowMeta,
              parentWorkflowName = message.parentWorkflowName,
              parentWorkflowId = message.parentWorkflowId,
              parentMethodRunId = message.parentMethodRunId,
              clientWaiting = message.clientWaiting,
              emitterName = message.emitterName,
          )

          launch { producer.sendToWorkflowCmd(dispatchWorkflow) }

          // send global timeout if needed
          val timeout = message.methodTimeout

          if (timeout != null && message.parentWorkflowId != null) {
            val childMethodTimedOut = ChildMethodTimedOut(
                childMethodTimedOutError = MethodTimedOutError(
                    workflowName = message.workflowName,
                    workflowId = message.workflowId,
                    methodName = message.methodName,
                    methodRunId = MethodRunId.from(message.workflowId),
                ),
                workflowName = message.parentWorkflowName ?: thisShouldNotHappen(),
                workflowId = message.parentWorkflowId ?: thisShouldNotHappen(),
                methodRunId = message.parentMethodRunId ?: thisShouldNotHappen(),
                emitterName = emitterName,
            )
            launch { producer.sendToWorkflowEngineLater(childMethodTimedOut, timeout) }
          }
        }
        // Another running workflow instance exist with same custom id
        1 -> {
          logger.debug {
            "A workflow '${message.workflowName}(${ids.first()})' already exists with tag '${message.workflowTag}'"
          }

          // if needed, we inform workflowEngine that a client is waiting for its result
          if (message.clientWaiting) {
            val waitWorkflow = WaitWorkflow(
                methodRunId = MethodRunId.from(ids.first()),
                workflowName = message.workflowName,
                workflowId = ids.first(),
                emitterName = message.emitterName,
            )

            launch { producer.sendToWorkflowCmd(waitWorkflow) }
          }

          Unit
        }
        // multiple running workflow instance exist with same custom id
        else -> thisShouldNotHappen(
            "Workflow '${message.workflowName}' with customId '${message.workflowTag}' has multiple ids: ${ids.joinToString()}",
        )
      }
    }
  }

  private suspend fun dispatchMethodByTag(message: DispatchMethodByTag) {
    val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)

    // with coroutineScope, we send messages in parallel and wait for all of them to be processed
    coroutineScope {
      when (ids.isEmpty()) {
        true -> discardTagWithoutIds(message)

        false -> ids.forEach {
          // parent workflow already applied method to self
          if (it != message.parentWorkflowId) {
            val dispatchMethod = DispatchMethodOnRunningWorkflow(
                workflowName = message.workflowName,
                workflowId = it,
                methodRunId = message.methodRunId,
                methodName = message.methodName,
                methodParameters = message.methodParameters,
                methodParameterTypes = message.methodParameterTypes,
                parentWorkflowId = message.parentWorkflowId,
                parentWorkflowName = message.parentWorkflowName,
                parentMethodRunId = message.parentMethodRunId,
                clientWaiting = false,
                emitterName = emitterName,
            )
            launch { producer.sendToWorkflowCmd(dispatchMethod) }

            // set timeout if any,
            if (message.methodTimeout != null && message.parentWorkflowId != null) {
              val childMethodTimedOut = ChildMethodTimedOut(
                  childMethodTimedOutError = MethodTimedOutError(
                      workflowName = message.workflowName,
                      workflowId = it,
                      methodName = message.methodName,
                      methodRunId = message.methodRunId,
                  ),
                  workflowName = message.parentWorkflowName!!,
                  workflowId = message.parentWorkflowId!!,
                  methodRunId = message.parentMethodRunId!!,
                  emitterName = emitterName,
              )

              launch {
                producer.sendToWorkflowEngineLater(
                    childMethodTimedOut,
                    message.methodTimeout!!,
                )
              }
            }
          }
        }
      }
    }
  }

  private suspend fun retryWorkflowTaskByTag(message: RetryWorkflowTaskByTag) {
    val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)

    // with coroutineScope, we send messages in parallel and wait for all of them to be processed
    coroutineScope {
      when (ids.isEmpty()) {
        true -> discardTagWithoutIds(message)

        false -> ids.forEach {
          val retryWorkflowTask = RetryWorkflowTask(
              workflowName = message.workflowName,
              workflowId = it,
              emitterName = emitterName,
          )
          launch { producer.sendToWorkflowCmd(retryWorkflowTask) }
        }
      }
    }
  }

  private suspend fun retryTaskByTag(message: RetryTasksByTag) {
    val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)

    // with coroutineScope, we send messages in parallel and wait for all of them to be processed
    coroutineScope {
      when (ids.isEmpty()) {
        true -> discardTagWithoutIds(message)

        false -> ids.forEach {
          val retryTasks = RetryTasks(
              taskId = message.taskId,
              taskStatus = message.taskStatus,
              serviceName = message.serviceName,
              workflowName = message.workflowName,
              workflowId = it,
              emitterName = emitterName,
          )
          launch { producer.sendToWorkflowCmd(retryTasks) }
        }
      }
    }
  }

  private suspend fun completeTimerByTag(message: CompleteTimersByTag) {
    val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)

    // with coroutineScope, we send messages in parallel and wait for all of them to be processed
    coroutineScope {
      when (ids.isEmpty()) {
        true -> discardTagWithoutIds(message)

        false -> ids.forEach {
          val completeTimers = CompleteTimers(
              methodRunId = message.methodRunId,
              workflowName = message.workflowName,
              workflowId = it,
              emitterName = emitterName,
          )
          launch { producer.sendToWorkflowCmd(completeTimers) }
        }
      }
    }
  }

  private suspend fun cancelWorkflowByTag(message: CancelWorkflowByTag) {
    val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)

    // with coroutineScope, we send messages in parallel and wait for all of them to be processed
    coroutineScope {
      when (ids.isEmpty()) {
        true -> discardTagWithoutIds(message)

        false -> ids.forEach {
          // parent workflow already applied method to self
          if (it != message.emitterWorkflowId) {
            val cancelWorkflow = CancelWorkflow(
                reason = message.reason,
                methodRunId = MethodRunId.from(it),
                workflowName = message.workflowName,
                workflowId = it,
                emitterName = emitterName,
            )
            launch { producer.sendToWorkflowCmd(cancelWorkflow) }
          }
        }
      }
    }
  }

  private suspend fun sendToChannelByTag(message: SendSignalByTag) {
    val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)

    // with coroutineScope, we send messages in parallel and wait for all of them to be processed
    coroutineScope {
      when (ids.isEmpty()) {
        true -> discardTagWithoutIds(message)

        false -> ids.forEach {
          // parent workflow already applied method to self
          if (it != message.emitterWorkflowId) {
            val sendSignal = SendSignal(
                channelName = message.channelName,
                signalId = message.signalId,
                signalData = message.signalData,
                channelTypes = message.channelTypes,
                workflowName = message.workflowName,
                workflowId = it,
                emitterName = emitterName,
            )
            launch { producer.sendToWorkflowCmd(sendSignal) }
          }
        }
      }
    }
  }

  private suspend fun addWorkflowTag(message: AddTagToWorkflow) {
    storage.addWorkflowId(message.workflowTag, message.workflowName, message.workflowId)
  }

  private suspend fun removeWorkflowTag(message: RemoveTagFromWorkflow) {
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
    coroutineScope { producer.sendToClient(workflowIdsByTag) }
  }

  private fun discardTagWithoutIds(message: WorkflowTagMessage) {
    logger.debug { "discarding as no id found for the provided tag: $message" }
  }
}
