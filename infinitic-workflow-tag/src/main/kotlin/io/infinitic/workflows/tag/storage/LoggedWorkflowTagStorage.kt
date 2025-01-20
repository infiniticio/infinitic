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
package io.infinitic.workflows.tag.storage

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import org.jetbrains.annotations.TestOnly

class LoggedWorkflowTagStorage(
  private val logger: KLogger,
  private val storage: WorkflowTagStorage
) : WorkflowTagStorage {

  override suspend fun getWorkflowIds(
    tag: WorkflowTag,
    workflowName: WorkflowName
  ): Set<WorkflowId> {
    val workflowIds = storage.getWorkflowIds(tag, workflowName)
    logger.debug {
      "TAG $tag - workflowName $workflowName - getWorkflowIds ${workflowIds.size} found"
    }

    return workflowIds
  }

  override suspend fun addWorkflowId(
    tag: WorkflowTag,
    workflowName: WorkflowName,
    workflowId: WorkflowId
  ) {
    logger.debug { "TAG $tag - name $workflowName - addWorkflowId $workflowId" }
    storage.addWorkflowId(tag, workflowName, workflowId)
  }

  override suspend fun removeWorkflowId(
    tag: WorkflowTag,
    workflowName: WorkflowName,
    workflowId: WorkflowId
  ) {
    logger.debug { "TAG $tag - name $workflowName - removeWorkflowId $workflowId" }
    storage.removeWorkflowId(tag, workflowName, workflowId)
  }

  override suspend fun getWorkflowIds(
    tagAndNames: Set<Pair<WorkflowTag, WorkflowName>>
  ): Map<Pair<WorkflowTag, WorkflowName>, Set<WorkflowId>> {
    val map = storage.getWorkflowIds(tagAndNames)
    map.forEach { (tagAndNames, workflowIds) ->
      logger.debug {
        "TAG ${tagAndNames.first} - workflowName ${tagAndNames.second} - getWorkflowIds ${workflowIds.size} found"
      }
    }
    return map
  }

  override suspend fun updateWorkflowIds(
    add: Map<Pair<WorkflowTag, WorkflowName>, Set<WorkflowId>>,
    remove: Map<Pair<WorkflowTag, WorkflowName>, Set<WorkflowId>>
  ) {
    add.forEach { (tagAndNames, workflowIds) ->
      workflowIds.forEach { workflowId ->
        logger.debug { "TAG ${tagAndNames.first} - name ${tagAndNames.second} - addWorkflowId $workflowId" }
      }
    }
    remove.forEach { (tagAndNames, workflowIds) ->
      workflowIds.forEach { workflowId ->
        logger.debug { "TAG ${tagAndNames.first} - name ${tagAndNames.second} - removeWorkflowId $workflowId" }
      }
    }
    storage.updateWorkflowIds(add, remove)
  }

  @TestOnly
  override fun flush() {
    logger.warn { "flushing workflowTagStorage" }
    storage.flush()
  }
}
