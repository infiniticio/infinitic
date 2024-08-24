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
package io.infinitic.tasks.tag.storage

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.tasks.data.DelegatedTaskData
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import org.jetbrains.annotations.TestOnly

class LoggedTaskTagStorage(
  private val logger: KLogger,
  private val storage: TaskTagStorage
) : TaskTagStorage {

  override suspend fun getTaskIdsForTag(tag: TaskTag, serviceName: ServiceName): Set<TaskId> {
    logger.trace { "tag $tag - taskName $serviceName - getting TaskIds" }
    val taskIds = storage.getTaskIdsForTag(tag, serviceName)
    logger.debug { "tag $tag - taskName $serviceName - got TaskIds $taskIds" }
    return taskIds
  }

  override suspend fun addTaskIdToTag(tag: TaskTag, serviceName: ServiceName, taskId: TaskId) {
    logger.trace { "tag $tag - name $serviceName - adding TaskId $taskId" }
    storage.addTaskIdToTag(tag, serviceName, taskId)
    logger.debug { "tag $tag - name $serviceName - added TaskId $taskId" }
  }

  override suspend fun removeTaskIdFromTag(tag: TaskTag, serviceName: ServiceName, taskId: TaskId) {
    logger.trace { "tag $tag - name $serviceName - removing TaskId $taskId" }
    storage.removeTaskIdFromTag(tag, serviceName, taskId)
    logger.debug { "tag $tag - name $serviceName - removed TaskId $taskId" }
  }

  override suspend fun setDelegatedTaskData(taskId: TaskId, data: DelegatedTaskData) {
    logger.trace { "taskId $taskId - setting DelegatedTaskData $data" }
    storage.setDelegatedTaskData(taskId, data)
    logger.debug { "taskId $taskId - set DelegatedTaskData $data" }
  }

  override suspend fun delDelegatedTaskData(taskId: TaskId) {
    logger.trace { "taskId $taskId - deleting DelegatedTaskData" }
    storage.delDelegatedTaskData(taskId)
    logger.debug { "taskId $taskId - deleted DelegatedTaskData" }
  }

  override suspend fun getDelegatedTaskData(taskId: TaskId): DelegatedTaskData? {
    logger.trace { "taskId $taskId - getting DelegatedTaskData" }
    val asyncData = storage.getDelegatedTaskData(taskId)
    logger.debug { "taskId $taskId - got DelegatedTaskData $asyncData" }
    return asyncData
  }

  @TestOnly
  override fun flush() {
    logger.warn { "flushing taskTagStorage" }
    storage.flush()
  }
}
