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

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.transport.logged.formatLog
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import org.jetbrains.annotations.TestOnly

class LoggedWorkflowStateStorage(
  private val logger: KLogger,
  private val storage: WorkflowStateStorage,
) : WorkflowStateStorage {

  override suspend fun getState(workflowId: WorkflowId): WorkflowState? {
    logger.trace { formatLog(workflowId, "Getting State...") }
    val workflowState = storage.getState(workflowId)
    logger.debug { formatLog(workflowId, "Get state:", workflowState) }
    return workflowState
  }

  override suspend fun putState(workflowId: WorkflowId, workflowState: WorkflowState?) {
    logger.trace { formatLog(workflowId, "Putting State...") }
    storage.putState(workflowId, workflowState)
    logger.debug { formatLog(workflowId, "Put state:", workflowState) }
  }

  override suspend fun getStates(workflowIds: List<WorkflowId>): Map<WorkflowId, WorkflowState?> {
    workflowIds.forEach { logger.trace { formatLog(it, "Getting State...") } }
    val workflowStates = storage.getStates(workflowIds)
    workflowIds.forEach { logger.debug { formatLog(it, "Get state:", workflowStates[it]) } }
    return workflowStates
  }

  override suspend fun putStates(workflowStates: Map<WorkflowId, WorkflowState?>) {
    workflowStates.forEach { logger.trace { formatLog(it.key, "Putting State...") } }
    storage.putStates(workflowStates)
    workflowStates.forEach { logger.debug { formatLog(it.key, "Put state:", it.value) } }
  }

  override suspend fun putStateWithVersion(
    workflowId: WorkflowId,
    workflowState: WorkflowState?,
    expectedVersion: Long
  ): Boolean {
    logger.trace { formatLog(workflowId, "Putting State (version $expectedVersion)...") }
    val result = storage.putStateWithVersion(workflowId, workflowState, expectedVersion)
    val msg = when (result) {
      true -> "Put state (version $expectedVersion):"
      false -> "Failed Putting state (version $expectedVersion):"
    }
    when (result) {
      true -> logger.debug { formatLog(workflowId, msg, workflowState) }
      false -> logger.warn { formatLog(workflowId, msg, workflowState) }
    }
    return result
  }

  override suspend fun getStateAndVersion(workflowId: WorkflowId): Pair<WorkflowState?, Long> {
    logger.trace { formatLog(workflowId, "Getting State and version...") }
    val result = storage.getStateAndVersion(workflowId)
    logger.debug {
      formatLog(workflowId, "Get state (version ${result.second}):", result.first)
    }
    return result
  }

  @TestOnly
  override fun flush() {
    logger.warn { "Flushing workflowStateStorage" }
    storage.flush()
  }

}
