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

package io.infinitic.workflows.engine.storage.states

import io.infinitic.common.storage.Flushable
import io.infinitic.common.storage.keyValue.KeyValueCache
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.state.WorkflowState

/**
 * This WorkflowStateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
open class WorkflowStateKeyValueStorage(
    private val storage: KeyValueStorage,
    private val cache: KeyValueCache<WorkflowState>
) : WorkflowStateStorage {

    override val createStateFn: CreateWorkflowState = { workflowId: WorkflowId, state: WorkflowState ->
        val key = getWorkflowStateKey(workflowId)
        cache.set(key, state)
        storage.putState(key, state.toByteBuffer())
    }

    override val getStateFn: GetWorkflowState = { workflowId: WorkflowId ->
        val key = getWorkflowStateKey(workflowId)
        cache.get(key) ?: run {
            logger.debug("workflowId {} - getStateFn - absent from cache, get from storage", workflowId)
            storage.getState(key)?.let { WorkflowState.fromByteBuffer(it) }
        }
    }

    override val updateStateFn: UpdateWorkflowState = { workflowId: WorkflowId, state: WorkflowState ->
        val key = getWorkflowStateKey(workflowId)
        cache.set(key, state)
        storage.putState(key, state.toByteBuffer())
    }

    override val deleteStateFn: DeleteWorkflowState = { workflowId: WorkflowId ->
        val key = getWorkflowStateKey(workflowId)
        cache.delete(key)
        storage.deleteState(key)
    }

    /*
    Use for tests
     */
    fun flush() {
        if (storage is Flushable) {
            storage.flush()
        } else {
            throw Exception("Storage non flushable")
        }
        if (cache is Flushable) {
            cache.flush()
        } else {
            throw Exception("Cache non flushable")
        }
    }

    private fun getWorkflowStateKey(workflowId: WorkflowId) = "workflow.state.$workflowId"
}
