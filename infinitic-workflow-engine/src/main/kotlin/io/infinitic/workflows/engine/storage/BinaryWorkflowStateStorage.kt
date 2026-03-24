/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of the rights granted to you
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

import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.engine.state.toConvenienceData
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorageWithConvenience
import io.infinitic.storage.keyValue.KeyValueStorage
import io.infinitic.storage.keyValue.WrappedKeyValueStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.annotations.TestOnly

/**
 * WorkflowStateStorage implementation
 *
 * Workflow state are converted to Avro bytes array and saved in a key value store by WorkflowId
 *
 * Optionally stores convenience fields (status, meta, tags) alongside the state for easier querying.
 * These fields are stored as separate keys and their storage failures are logged but don't affect
 * the main workflow state storage operation.
 *
 * Any exception thrown by the storage is wrapped into KeyValueStorageException
 */
class BinaryWorkflowStateStorage(
  storage: KeyValueStorage,
  private val storeConvenienceFields: Boolean = true
) : WorkflowStateStorage, WorkflowStateStorageWithConvenience {

  // wrap any exception into KeyValueStorageException
  private val storage = WrappedKeyValueStorage(storage)

  override fun supportsConvenienceFields(): Boolean = storeConvenienceFields

  override suspend fun putStateWithVersion(
    workflowId: WorkflowId,
    workflowState: WorkflowState?,
    expectedVersion: Long
  ): Boolean {
    val key = getWorkflowStateKey(workflowId)
    val result = storage.putWithVersion(key, workflowState?.toByteArray(), expectedVersion)

    // Store convenience fields if enabled and state is not null
    if (result && storeConvenienceFields && workflowState != null) {
      try {
        storeConvenienceFieldsForWorkflow(workflowId, workflowState)
      } catch (e: Exception) {
        // Log but don't fail the operation if convenience fields storage fails
        // These are optional fields for querying convenience
        println("Warning: Failed to store convenience fields for workflow $workflowId: ${e.message}")
      }
    }

    return result
  }

  private suspend fun storeConvenienceFieldsForWorkflow(
    workflowId: WorkflowId,
    workflowState: WorkflowState
  ) {
    val convenienceData = workflowState.toConvenienceData()

    // Store each convenience field as a separate key
    val convenienceKeys = mapOf(
        getWorkflowStatusKey(workflowId) to convenienceData.status.toByteArray(),
        getWorkflowMetaKey(workflowId) to convenienceData.meta.toByteArray(),
        getWorkflowTagsKey(workflowId) to convenienceData.tags.toByteArray()
    )

    storage.put(convenienceKeys)
  }

  override suspend fun getStateAndVersion(workflowId: WorkflowId): Pair<WorkflowState?, Long> {
    val key = getWorkflowStateKey(workflowId)
    val (bytes, version) = storage.getStateAndVersion(key)
    return Pair(bytes?.let { WorkflowState.fromByteArray(it) }, version)
  }

  override suspend fun putStatesWithVersions(
    workflowStates: Map<WorkflowId, Pair<WorkflowState?, Long>>
  ): Map<WorkflowId, Boolean> {
    val map = coroutineScope {
      workflowStates
          .mapKeys { getWorkflowStateKey(it.key) }
          .mapValues { async { Pair(it.value.first?.toByteArray(), it.value.second) } }
          .mapValues { it.value.await() }
    }
    val results = storage.putWithVersions(map)
    return workflowStates.keys.associateWith { workflowId ->
      results[getWorkflowStateKey(workflowId)] ?: false
    }
  }

  override suspend fun getStatesAndVersions(
    workflowIds: List<WorkflowId>
  ): Map<WorkflowId, Pair<WorkflowState?, Long>> {
    val keys = workflowIds.map { getWorkflowStateKey(it) }.toSet()
    val results = coroutineScope {
      storage.getStatesAndVersions(keys)
          .mapValues {
            async {
              Pair(
                  it.value.first?.let { bytes -> WorkflowState.fromByteArray(bytes) },
                  it.value.second,
              )
            }
          }
          .mapValues { it.value.await() }
    }
    return workflowIds.associateWith { workflowId ->
      results[getWorkflowStateKey(workflowId)] ?: Pair(null, 0L)
    }
  }

  @TestOnly
  override fun flush() = storage.flush()

  internal fun getWorkflowStateKey(workflowId: WorkflowId) = "workflow.state.$workflowId"

  private fun getWorkflowStatusKey(workflowId: WorkflowId) = "workflow.status.$workflowId"

  private fun getWorkflowMetaKey(workflowId: WorkflowId) = "workflow.meta.$workflowId"

  private fun getWorkflowTagsKey(workflowId: WorkflowId) = "workflow.tags.$workflowId"
}
