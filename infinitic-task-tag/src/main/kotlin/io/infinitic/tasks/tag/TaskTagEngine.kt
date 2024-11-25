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
package io.infinitic.tasks.tag

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.TaskCompleted
import io.infinitic.common.clients.messages.TaskIdsByTag
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.tags.messages.AddTaskIdToTag
import io.infinitic.common.tasks.tags.messages.CancelTaskByTag
import io.infinitic.common.tasks.tags.messages.CompleteDelegatedTask
import io.infinitic.common.tasks.tags.messages.GetTaskIdsByTag
import io.infinitic.common.tasks.tags.messages.RemoveTaskIdFromTag
import io.infinitic.common.tasks.tags.messages.RetryTaskByTag
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.tasks.tags.messages.SetDelegatedTaskData
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.logged.LoggerWithCounter
import io.infinitic.common.workflows.engine.messages.RemoteTaskCompleted
import kotlinx.coroutines.coroutineScope

class TaskTagEngine(
  private val _storage: TaskTagStorage,
  private val _producer: InfiniticProducer
) {
  private val emitterName = _producer.emitterName

  suspend fun process(message: ServiceTagMessage, publishTime: MillisInstant) = process(
      storage = _storage,
      producer = _producer,
      message = message,
      publishTime = publishTime,
  )

  private suspend fun process(
    storage: TaskTagStorage,
    producer: InfiniticProducer,
    message: ServiceTagMessage,
    publishTime: MillisInstant
  ) = coroutineScope {
    when (message) {
      is AddTaskIdToTag -> addTaskIdToTag(storage, message)
      is RemoveTaskIdFromTag -> removeTaskIdFromTag(storage, message)
      is CancelTaskByTag -> TODO()
      is RetryTaskByTag -> thisShouldNotHappen()
      is SetDelegatedTaskData -> setDelegatedTaskData(storage, message)
      is CompleteDelegatedTask -> completeDelegateTask(storage, producer, message, publishTime)
      is GetTaskIdsByTag -> getTaskIds(storage, producer, message)
      else -> thisShouldNotHappen()
    }
  }

  private suspend fun addTaskIdToTag(
    storage: TaskTagStorage,
    message: AddTaskIdToTag
  ) {
    storage.addTaskId(message.taskTag, message.serviceName, message.taskId)
  }

  private suspend fun removeTaskIdFromTag(
    storage: TaskTagStorage,
    message: RemoveTaskIdFromTag
  ) {
    storage.removeTaskId(message.taskTag, message.serviceName, message.taskId)
  }

  private suspend fun setDelegatedTaskData(
    storage: TaskTagStorage,
    message: SetDelegatedTaskData
  ) {
    storage.updateDelegatedTaskData(message.taskId, message.delegatedTaskData)
  }

  private suspend fun completeDelegateTask(
    storage: TaskTagStorage,
    producer: InfiniticProducer,
    message: CompleteDelegatedTask,
    publishTime: MillisInstant
  ) {
    storage.getDelegatedTaskData(message.taskId)?.let {
      // send to waiting client
      TaskCompleted.from(it, message.returnValue, emitterName)?.let {
        with(producer) { it.sendTo(ClientTopic) }
      }
      // send to waiting workflow
      RemoteTaskCompleted.from(it, message.returnValue, emitterName, publishTime)?.let {
        with(producer) { it.sendTo(WorkflowStateEngineTopic) }
      }
      // delete delegatedTaskData
      storage.updateDelegatedTaskData(message.taskId, null)
    }
      ?: logger.warn { "Discarding message as no DelegatedTaskData found $message" }
  }

  private suspend fun getTaskIds(
    storage: TaskTagStorage,
    producer: InfiniticProducer,
    message: GetTaskIdsByTag
  ) {
    val taskIds = storage.getTaskIds(message.taskTag, message.serviceName)

    val taskIdsByTag = TaskIdsByTag(
        recipientName = ClientName.from(message.emitterName),
        serviceName = message.serviceName,
        taskTag = message.taskTag,
        taskIds = taskIds,
        emitterName = emitterName,
    )

    with(producer) { taskIdsByTag.sendTo(ClientTopic) }
  }

  companion object {
    val logger = LoggerWithCounter(KotlinLogging.logger {})
  }
}
