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
package io.infinitic.workflows.engine.storage

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import org.jetbrains.annotations.TestOnly

class LoggedWorkflowStateStorage(
  logName: String,
  private val storage: WorkflowStateStorage,
) : WorkflowStateStorage {

  private val logger = KotlinLogging.logger(logName)

  override suspend fun getState(workflowId: WorkflowId): WorkflowState? {
    val workflowState = storage.getState(workflowId)
    logger.debug { "Id $workflowId - GetState $workflowState" }

    return workflowState
  }

  override suspend fun putState(workflowId: WorkflowId, workflowState: WorkflowState) {
    logger.debug { "Id $workflowId - PutState $workflowState" }
    storage.putState(workflowId, workflowState)
  }

  override suspend fun delState(workflowId: WorkflowId) {
    logger.debug { "Id $workflowId - DelState" }
    storage.delState(workflowId)
  }

  @TestOnly
  override fun flush() {
    logger.debug { "flushing workflowStateStorage" }
    storage.flush()
  }
}
