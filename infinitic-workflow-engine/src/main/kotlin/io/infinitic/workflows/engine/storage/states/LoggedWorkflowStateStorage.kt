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

import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.state.WorkflowState
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LoggedWorkflowStateStorage(
    val storage: WorkflowStateStorage
) : WorkflowStateStorage {

    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    override suspend fun getState(workflowId: WorkflowId): WorkflowState? {
        val state = storage.getState(workflowId)
        logger.debug("workflowId {} - getState {}", workflowId, state)

        return state
    }

    override suspend fun putState(workflowId: WorkflowId, state: WorkflowState) {
        logger.debug("taskId {} - putState {}", workflowId, state)
        storage.putState(workflowId, state)
    }
    override suspend fun delState(workflowId: WorkflowId) {
        logger.debug("workflowId {} - delState", workflowId)
        storage.delState(workflowId)
    }
}
