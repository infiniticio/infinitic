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
package io.infinitic.workflows.tag.storage

import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import io.infinitic.storage.keySet.KeySetStorage
import io.infinitic.storage.keySet.WrappedKeySetStorage
import org.jetbrains.annotations.TestOnly

/**
 * WorkflowTagStorage implementation
 *
 * LastMessageId is saved in a key value store in a binary format WorkflowIds are saved in a key set
 * store in a binary format
 *
 * Any exception thrown by the storage is wrapped into KeyValueStorageException
 */
class BinaryWorkflowTagStorage(keySetStorage: KeySetStorage) :
  WorkflowTagStorage {

  private val keySetStorage = WrappedKeySetStorage(keySetStorage)

  override suspend fun getWorkflowIds(
    tag: WorkflowTag,
    workflowName: WorkflowName
  ): Set<WorkflowId> {
    val key = getTagSetIdsKey(tag, workflowName)
    return keySetStorage.get(key).map { WorkflowId(String(it)) }.toSet()
  }

  override suspend fun addWorkflowId(
    tag: WorkflowTag,
    workflowName: WorkflowName,
    workflowId: WorkflowId
  ) {
    val key = getTagSetIdsKey(tag, workflowName)
    keySetStorage.add(key, workflowId.toString().toByteArray())
  }

  override suspend fun removeWorkflowId(
    tag: WorkflowTag,
    workflowName: WorkflowName,
    workflowId: WorkflowId
  ) {
    val key = getTagSetIdsKey(tag, workflowName)
    keySetStorage.remove(key, workflowId.toString().toByteArray())
  }

  override suspend fun getWorkflowIds(tagAndNames: Set<Pair<WorkflowTag, WorkflowName>>): Map<Pair<WorkflowTag, WorkflowName>, Set<WorkflowId>> {
    val keys = tagAndNames.associateBy { getTagSetIdsKey(it.first, it.second) }

    return keySetStorage.get(keys.keys.toSet())
        .mapKeys { keys[it.key]!! }
        .mapValues { entry -> entry.value.map { WorkflowId(String(it)) }.toSet() }
  }

  override suspend fun updateWorkflowIds(
    add: Map<Pair<WorkflowTag, WorkflowName>, Set<WorkflowId>>,
    remove: Map<Pair<WorkflowTag, WorkflowName>, Set<WorkflowId>>
  ) {
    val addData = add
        .mapKeys { getTagSetIdsKey(it.key.first, it.key.second) }
        .mapValues { it.value.map { workflowId -> workflowId.toString().toByteArray() }.toSet() }

    val removeData = remove
        .mapKeys { getTagSetIdsKey(it.key.first, it.key.second) }
        .mapValues { it.value.map { workflowId -> workflowId.toString().toByteArray() }.toSet() }

    keySetStorage.update(add = addData, remove = removeData)
  }

  internal fun getTagSetIdsKey(tag: WorkflowTag, workflowName: WorkflowName) =
      "workflow:$workflowName|tag:$tag|setIds"

  /** Flush storage (testing purpose) */
  @TestOnly
  override fun flush() {
    keySetStorage.flush()
  }
}
