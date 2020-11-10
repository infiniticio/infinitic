// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.engines.workflows.storage

import io.infinitic.storage.api.KeyValueStorage
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.states.WorkflowState

/**
 * This WorkflowStateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
open class WorkflowStateKeyValueStorage(private val storage: KeyValueStorage) : WorkflowStateStorage {

    override fun createState(workflowId: WorkflowId, state: WorkflowState) {
        storage.putState(
            getWorkflowStateKey(workflowId),
            state.toByteBuffer()
        )
    }

    override fun getState(workflowId: WorkflowId) = storage
        .getState(getWorkflowStateKey(workflowId))
        ?.let { WorkflowState.fromByteBuffer(it) }

    override fun updateState(workflowId: WorkflowId, state: WorkflowState) {
        storage.putState(
            getWorkflowStateKey(workflowId),
            state.toByteBuffer()
        )
    }

    override fun deleteState(workflowId: WorkflowId) {
        storage.deleteState(getWorkflowStateKey(workflowId))
    }

    private fun getWorkflowStateKey(workflowId: WorkflowId) = "workflow.state.$workflowId"
}
