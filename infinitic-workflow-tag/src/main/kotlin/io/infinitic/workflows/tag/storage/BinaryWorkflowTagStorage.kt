/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.workflows.tag.storage

import io.infinitic.common.storage.keySet.KeySetStorage
import io.infinitic.common.storage.keySet.WrappedKeySetStorage
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.storage.keyValue.WrappedKeyValueStorage
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import org.jetbrains.annotations.TestOnly

/**
 * WorkflowTagStorage implementation
 *
 * LastMessageId is saved in a key value store in a binary format
 * WorkflowIds are saved in a key set store in a binary format
 *
 * Any exception thrown by the storage is wrapped into KeyValueStorageException
 */
class BinaryWorkflowTagStorage(
    keyValueStorage: KeyValueStorage,
    keySetStorage: KeySetStorage,
) : WorkflowTagStorage {

    private val keyValueStorage = WrappedKeyValueStorage(keyValueStorage)
    private val keySetStorage = WrappedKeySetStorage(keySetStorage)

    override suspend fun getWorkflowIds(tag: WorkflowTag, workflowName: WorkflowName): Set<WorkflowId> {
        val key = getTagSetIdsKey(tag, workflowName)
        return keySetStorage
            .get(key)
            .map { WorkflowId(String(it)) }
            .toSet()
    }

    override suspend fun addWorkflowId(tag: WorkflowTag, workflowName: WorkflowName, workflowId: WorkflowId) {
        val key = getTagSetIdsKey(tag, workflowName)
        keySetStorage.add(key, workflowId.toString().toByteArray())
    }

    override suspend fun removeWorkflowId(tag: WorkflowTag, workflowName: WorkflowName, workflowId: WorkflowId) {
        val key = getTagSetIdsKey(tag, workflowName)
        keySetStorage.remove(key, workflowId.toString().toByteArray())
    }

    private fun getTagSetIdsKey(tag: WorkflowTag, workflowName: WorkflowName) = "workflow:$workflowName|tag:$tag|setIds"

    /**
     * Flush storage (testing purpose)
     */
    @TestOnly
    override fun flush() {
        keyValueStorage.flush()
        keySetStorage.flush()
    }
}
