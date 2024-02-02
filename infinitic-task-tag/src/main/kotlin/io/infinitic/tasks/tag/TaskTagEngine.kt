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
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.tags.messages.AddTaskIdToTag
import io.infinitic.common.tasks.tags.messages.CancelTaskByTag
import io.infinitic.common.tasks.tags.messages.CompleteAsyncTask
import io.infinitic.common.tasks.tags.messages.GetTaskIdsByTag
import io.infinitic.common.tasks.tags.messages.RemoveTaskIdFromTag
import io.infinitic.common.tasks.tags.messages.RetryTaskByTag
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.tasks.tags.messages.SetAsyncTaskData
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.transport.WorkflowEngineTopic
import io.infinitic.common.workflows.engine.messages.RemoteTaskCompleted
import io.infinitic.tasks.tag.storage.LoggedTaskTagStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class TaskTagEngine(
  storage: TaskTagStorage,
  producerAsync: InfiniticProducerAsync
) {
  private lateinit var scope: CoroutineScope

  private val storage = LoggedTaskTagStorage(storage).apply {
    logName = this::class.java.name
  }

  private val producer = LoggedInfiniticProducer(this::class.java.name, producerAsync)

  private val logger = KotlinLogging.logger(this::class.java.name)

  private val emitterName by lazy { EmitterName(producer.name) }

  // coroutineScope let send messages in parallel
  // it's important as we can have a lot of them
  suspend fun handle(message: ServiceTagMessage, publishTime: MillisInstant) = coroutineScope {
    logger.debug { "receiving $message" }

    scope = this

    when (message) {
      is AddTaskIdToTag -> addTaskIdToTag(message)
      is RemoveTaskIdFromTag -> removeTaskIdFromTag(message)
      is CancelTaskByTag -> TODO()
      is RetryTaskByTag -> thisShouldNotHappen()
      is SetAsyncTaskData -> setAsyncTaskData(message)
      is CompleteAsyncTask -> completeAsyncTask(message, publishTime)
      is GetTaskIdsByTag -> getTaskIds(message)
      else -> thisShouldNotHappen()
    }
  }

  private suspend fun addTaskIdToTag(message: AddTaskIdToTag) {
    storage.addTaskIdToTag(message.taskTag, message.serviceName, message.taskId)
  }

  private suspend fun removeTaskIdFromTag(message: RemoveTaskIdFromTag) {
    storage.removeTaskIdFromTag(message.taskTag, message.serviceName, message.taskId)
  }

  private suspend fun setAsyncTaskData(message: SetAsyncTaskData) {
    storage.setAsyncTaskData(message.taskId, message.asyncTaskData)
  }

  private suspend fun completeAsyncTask(
    message: CompleteAsyncTask,
    publishTime: MillisInstant
  ) {
    storage.getAsyncTaskData(message.taskId)?.let {
      coroutineScope {
        // send to waiting client
        TaskCompleted.from(it, message.returnValue, message.emitterName)?.let {
          scope.launch { with(producer) { it.sendTo(ClientTopic) } }
        }
        // send to waiting workflow
        RemoteTaskCompleted.from(it, message.returnValue, message.emitterName, publishTime)?.let {
          scope.launch { with(producer) { it.sendTo(WorkflowEngineTopic) } }
        }
      }
      // delete asyncTaskData
      storage.delAsyncTaskData(message.taskId)
    }
  }

  private suspend fun getTaskIds(message: GetTaskIdsByTag) {
    val taskIds = storage.getTaskIdsForTag(message.taskTag, message.serviceName)

    val taskIdsByTag = TaskIdsByTag(
        recipientName = ClientName.from(message.emitterName),
        serviceName = message.serviceName,
        taskTag = message.taskTag,
        taskIds = taskIds,
        emitterName = emitterName,
    )

    scope.launch { with(producer) { taskIdsByTag.sendTo(ClientTopic) } }
  }
}
