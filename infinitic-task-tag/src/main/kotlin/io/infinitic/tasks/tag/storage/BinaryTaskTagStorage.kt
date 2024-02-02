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

import io.infinitic.common.tasks.data.AsyncTaskData
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.storage.keySet.KeySetStorage
import io.infinitic.storage.keySet.WrappedKeySetStorage
import io.infinitic.storage.keyValue.KeyValueStorage
import io.infinitic.storage.keyValue.WrappedKeyValueStorage
import org.jetbrains.annotations.TestOnly

/**
 * TaskTagStorage implementation
 *
 * LastMessageId is saved in a key value store in a binary format TaskIds are saved in a key set
 * store in a binary format
 *
 * Any exception thrown by the storage is wrapped into KeyValueStorageException
 */
class BinaryTaskTagStorage(keyValueStorage: KeyValueStorage, keySetStorage: KeySetStorage) :
  TaskTagStorage {

  private val keyValueStorage = WrappedKeyValueStorage(keyValueStorage)
  private val keySetStorage = WrappedKeySetStorage(keySetStorage)

  override suspend fun getTaskIdsForTag(tag: TaskTag, serviceName: ServiceName): Set<TaskId> {
    val key = getTaskIdsByTagKey(tag, serviceName)
    return keySetStorage.get(key).map { TaskId(String(it)) }.toSet()
  }

  override suspend fun addTaskIdToTag(tag: TaskTag, serviceName: ServiceName, taskId: TaskId) {
    val key = getTaskIdsByTagKey(tag, serviceName)
    keySetStorage.add(key, taskId.toString().toByteArray())
  }

  override suspend fun removeTaskIdFromTag(tag: TaskTag, serviceName: ServiceName, taskId: TaskId) {
    val key = getTaskIdsByTagKey(tag, serviceName)
    keySetStorage.remove(key, taskId.toString().toByteArray())
  }

  override suspend fun setAsyncTaskData(taskId: TaskId, data: AsyncTaskData) {
    val key = getAsyncTaskDataKey(taskId)
    keyValueStorage.put(key, data.toByteArray())
  }

  override suspend fun delAsyncTaskData(taskId: TaskId) {
    val key = getAsyncTaskDataKey(taskId)
    keyValueStorage.del(key)
  }

  override suspend fun getAsyncTaskData(taskId: TaskId): AsyncTaskData? {
    val key = getAsyncTaskDataKey(taskId)
    return keyValueStorage.get(key)?.let { AsyncTaskData.fromByteArray(it) }

  }

  private fun getTaskIdsByTagKey(tag: TaskTag, serviceName: ServiceName) =
      "task:$serviceName|tag:$tag|setIds"

  private fun getAsyncTaskDataKey(taskId: TaskId) = "taskId:$taskId|asyncTaskData"

  @TestOnly
  override fun flush() {
    keyValueStorage.flush()
    keySetStorage.flush()
  }
}
