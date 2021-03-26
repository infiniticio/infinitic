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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This WorkflowStateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
open class WorkflowStateCachedKeyStorage(
    private val storage: KeyValueStorage,
    private val cache: KeyValueCache<WorkflowState>
) : WorkflowStateStorage {

    val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    override suspend fun getState(workflowId: WorkflowId): WorkflowState? {
        val key = getWorkflowStateKey(workflowId)
        val workflowState = cache.getValue(key) ?: run {
            logger.debug("workflowId {} - getStateFn - absent from cache, get from storage", workflowId)
            storage.getValue(key)
                ?.let { WorkflowState.fromByteArray(it) }
                ?.also { cache.putValue(key, it) }
        }
        logger.debug("workflowId {} - getState {}", workflowId, workflowState)

        return workflowState
    }

    override suspend fun putState(workflowId: WorkflowId, workflowState: WorkflowState) {
        val key = getWorkflowStateKey(workflowId)
        cache.putValue(key, workflowState)
        storage.putValue(key, workflowState.toByteArray())
        logger.debug("workflowId {} - updateState {}", workflowId, workflowState)
    }

    override suspend fun delState(workflowId: WorkflowId) {
        val key = getWorkflowStateKey(workflowId)
        cache.delValue(key)
        storage.delValue(key)
        logger.debug("workflowId {} - deleteState", workflowId)
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
