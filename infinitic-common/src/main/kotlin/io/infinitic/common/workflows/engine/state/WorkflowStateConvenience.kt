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
package io.infinitic.common.workflows.engine.state

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

/**
 * Container for convenience data extracted from WorkflowState.
 * These fields are stored alongside the workflow state for easier querying
 * without needing to deserialize the full state.
 */
data class WorkflowStateConvenienceData(
  /** Current status of the workflow */
  val status: String,
  /** Workflow meta as JSON string */
  val meta: String,
  /** Workflow tags as JSON array string */
  val tags: String
)

/**
 * Extracts convenience data from a WorkflowState.
 * This data can be stored in separate columns for efficient querying.
 */
fun WorkflowState.toConvenienceData(): WorkflowStateConvenienceData {
  val status = WorkflowStatus.from(this)

  // Convert meta to JSON string
  val metaJson = Json.encodeToString(
      kotlinx.serialization.json.JsonObject.serializer(),
      workflowMeta.toJson()
  )

  // Convert tags to JSON array string
  val tagsJsonArray = JsonArray(workflowTags.map { it.toJson() })
  val tagsJson = Json.encodeToString(JsonArray.serializer(), tagsJsonArray)

  return WorkflowStateConvenienceData(
      status = status.name,
      meta = metaJson,
      tags = tagsJson
  )
}
