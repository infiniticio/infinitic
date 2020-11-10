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

import io.infinitic.common.tasks.states.TaskState
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.states.WorkflowState
import io.infinitic.storage.api.Flushable
import java.util.concurrent.ConcurrentHashMap

/**
 * This WorkflowStateStorage implementation is used for tests.
 */
open class WorkflowStateInMemoryStorage() : WorkflowStateStorage {

    private val storage = ConcurrentHashMap<WorkflowId, ByteArray>()

    override fun createState(workflowId: WorkflowId, state: WorkflowState) {
        storage[workflowId] = state.toByteArray()
    }

    override fun getState(workflowId: WorkflowId) = storage[workflowId]
        ?.let { WorkflowState.fromByteArray(it) }

    override fun updateState(workflowId: WorkflowId, state: WorkflowState) {
        storage[workflowId] = state.toByteArray()
    }

    override fun deleteState(workflowId: WorkflowId) {
        storage.remove(workflowId)
    }

    fun flush() {
        storage.clear()
    }
}
