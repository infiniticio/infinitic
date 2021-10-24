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

package io.infinitic.tags.workflows.storage

import io.infinitic.common.data.MessageId
import io.infinitic.common.data.UUIDConversion.toByteArray
import io.infinitic.common.storage.Flushable
import io.infinitic.common.storage.keySet.KeySetStorage
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import org.jetbrains.annotations.TestOnly

/**
 * This StateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
class BinaryWorkflowTagStorage(
    private val keyValueStorage: KeyValueStorage,
    private val keySetStorage: KeySetStorage,
) : WorkflowTagStorage, Flushable {

    override suspend fun getLastMessageId(tag: WorkflowTag, workflowName: WorkflowName): MessageId? {
        val key = getTagMessageIdKey(tag, workflowName)

        return keyValueStorage.get(key)
            ?.let { MessageId.fromByteArray(it) }
    }

    override suspend fun setLastMessageId(tag: WorkflowTag, workflowName: WorkflowName, messageId: MessageId) {
        val key = getTagMessageIdKey(tag, workflowName)
        keyValueStorage.put(key, messageId.toByteArray())
    }

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

    private fun getTagMessageIdKey(tag: WorkflowTag, workflowName: WorkflowName) = "workflow:$workflowName|tag:$tag|messageId"

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
