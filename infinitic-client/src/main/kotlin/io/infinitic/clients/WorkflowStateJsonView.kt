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
package io.infinitic.clients

import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.workflows.engine.state.WorkflowState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

private val workflowStateJson = Json { prettyPrint = true }

internal fun WorkflowState.toClientJson(): String {
  val stateAsJson = workflowStateJson.encodeToJsonElement(WorkflowState.serializer(), this).jsonObject
  val expandedJson = JsonObject(stateAsJson + ("workflowMeta" to workflowMeta.toJson()))
      .expandSerializedData()

  return workflowStateJson.encodeToString(JsonElement.serializer(), expandedJson)
}

private fun JsonElement.expandSerializedData(): JsonElement = when (this) {
  is JsonObject -> decodeSerializedDataOrNull()?.toJson()
      ?: JsonObject(mapValues { (_, value) -> value.expandSerializedData() })

  is JsonArray -> JsonArray(map { it.expandSerializedData() })
  else -> this
}

private fun JsonObject.decodeSerializedDataOrNull(): SerializedData? {
  if ("bytes" !in this || "type" !in this) return null

  return runCatching {
    workflowStateJson.decodeFromJsonElement(SerializedData.serializer(), this)
  }.getOrNull()
}
