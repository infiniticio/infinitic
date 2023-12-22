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
import io.infinitic.common.clients.messages.TaskIdsByTag
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.tasks.tags.messages.AddTagToTask
import io.infinitic.common.tasks.tags.messages.CancelTaskByTag
import io.infinitic.common.tasks.tags.messages.GetTaskIdsByTag
import io.infinitic.common.tasks.tags.messages.RemoveTagFromTask
import io.infinitic.common.tasks.tags.messages.RetryTaskByTag
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.tasks.tag.storage.LoggedTaskTagStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class TaskTagEngine(
  storage: TaskTagStorage,
  producerAsync: InfiniticProducerAsync
) {
  private lateinit var scope: CoroutineScope

  private val storage = LoggedTaskTagStorage(javaClass.name, storage)

  private val producer = LoggedInfiniticProducer(javaClass.name, producerAsync)

  private val logger = KotlinLogging.logger(javaClass.name)

  private val emitterName by lazy { EmitterName(producer.name) }

  suspend fun handle(message: TaskTagMessage) {
    logger.debug { "receiving $message" }

    process(message)

    storage.setLastMessageId(message.taskTag, message.serviceName, message.messageId)
  }

  // coroutineScope let send messages in parallel
  // it's important as we can have a lot of them
  private suspend fun process(message: TaskTagMessage) = coroutineScope {
    scope = this

    when (message) {
      is AddTagToTask -> addTagToTask(message)
      is RemoveTagFromTask -> removeTagFromTask(message)
      is CancelTaskByTag -> cancelTaskByTag(message)
      is RetryTaskByTag -> retryTaskByTag(message)
      is GetTaskIdsByTag -> getTaskIds(message)
    }
  }

  private suspend fun addTagToTask(message: AddTagToTask) {
    storage.addTaskId(message.taskTag, message.serviceName, message.taskId)
  }

  private suspend fun removeTagFromTask(message: RemoveTagFromTask) {
    storage.removeTaskId(message.taskTag, message.serviceName, message.taskId)
  }

  private suspend fun retryTaskByTag(message: RetryTaskByTag) {
    // is not an idempotent action
    if (hasMessageAlreadyBeenHandled(message)) return

    val taskIds = storage.getTaskIds(message.taskTag, message.serviceName)
    when (taskIds.isEmpty()) {
      true -> discardTagWithoutIds(message)
      false -> taskIds.forEach { TODO() }
    }
  }

  private suspend fun cancelTaskByTag(message: CancelTaskByTag) {
    // is not an idempotent action
    if (hasMessageAlreadyBeenHandled(message)) return

    val ids = storage.getTaskIds(message.taskTag, message.serviceName)
    when (ids.isEmpty()) {
      true -> discardTagWithoutIds(message)
      false -> ids.forEach { TODO() }
    }
  }

  private suspend fun getTaskIds(message: GetTaskIdsByTag) {
    val taskIds = storage.getTaskIds(message.taskTag, message.serviceName)

    val taskIdsByTag = TaskIdsByTag(
        recipientName = ClientName.from(message.emitterName),
        serviceName = message.serviceName,
        taskTag = message.taskTag,
        taskIds = taskIds,
        emitterName = emitterName,
    )

    scope.launch { producer.sendToClient(taskIdsByTag) }
  }

  private suspend fun hasMessageAlreadyBeenHandled(message: TaskTagMessage) =
      when (storage.getLastMessageId(message.taskTag, message.serviceName)) {
        message.messageId -> {
          logger.info { "discarding as state already contains this messageId: $message" }
          true
        }

        else -> false
      }

  private fun discardTagWithoutIds(message: TaskTagMessage) {
    logger.debug { "discarding as no id found for the provided tag: $message" }
  }
}
