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

import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
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
 * Any exception thrown by the storage is wrapped into KeyValueStorageException
 */
class BinaryWorkflowStateStorage(storage: KeyValueStorage) : WorkflowStateStorage {

  // wrap any exception into KeyValueStorageException
  private val storage = WrappedKeyValueStorage(storage)

  override suspend fun getState(workflowId: WorkflowId): WorkflowState? {
    val key = getWorkflowStateKey(workflowId)
    return storage.get(key)?.let { WorkflowState.fromByteArray(it) }
  }

  override suspend fun putState(workflowId: WorkflowId, workflowState: WorkflowState?) {
    val key = getWorkflowStateKey(workflowId)
    storage.put(key, workflowState?.toByteArray())
  }

  override suspend fun getStates(workflowIds: List<WorkflowId>): Map<WorkflowId, WorkflowState?> {
    val keys = workflowIds.associateWith { getWorkflowStateKey(it) }
    val values = coroutineScope {
      storage.getSet(keys.values.toSet())
          .mapValues { async { it.value?.let { bytes -> WorkflowState.fromByteArray(bytes) } } }
          .mapValues { it.value.await() }
    }

    return keys.mapValues { values[it.value] }
  }

  override suspend fun putStates(workflowStates: Map<WorkflowId, WorkflowState?>) {
    val map = coroutineScope {
      workflowStates
          .mapKeys { getWorkflowStateKey(it.key) }
          .mapValues { async { it.value?.toByteArray() } }
          .mapValues { it.value.await() }
    }
    storage.putSet(map)
  }

  @TestOnly
  override fun flush() = storage.flush()

  private fun getWorkflowStateKey(workflowId: WorkflowId) = "workflow.state.$workflowId"
}
