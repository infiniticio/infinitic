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

package io.infinitic.workflows.engine.storage

import io.infinitic.common.storage.Flushable
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.state.WorkflowState

/**
 * This WorkflowStateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
open class BinaryWorkflowStateStorage(
    private val storage: KeyValueStorage,
) : WorkflowStateStorage, Flushable by storage {

    override suspend fun getState(workflowId: WorkflowId): WorkflowState? {
        val key = getWorkflowStateKey(workflowId)
        return storage.getValue(key)
            ?.let { WorkflowState.fromByteArray(it) }
    }

    override suspend fun putState(workflowId: WorkflowId, workflowState: WorkflowState) {
        val key = getWorkflowStateKey(workflowId)
        storage.putValue(key, workflowState.toByteArray())
    }

    override suspend fun delState(workflowId: WorkflowId) {
        val key = getWorkflowStateKey(workflowId)
        storage.delValue(key)
    }

    private fun getWorkflowStateKey(workflowId: WorkflowId) = "workflow.state.$workflowId"
}
