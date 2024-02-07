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
package io.infinitic.common.requester

import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.utils.JsonAble
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
@AvroNamespace("io.infinitic.data")
sealed interface Requester : JsonAble {
  override fun toJson(): JsonObject
}

@Serializable
@AvroNamespace("io.infinitic.data")
data class ClientRequester(
  val clientName: ClientName,
) : Requester {
  override fun toJson() = JsonObject(mapOf("clientName" to JsonPrimitive(clientName.toString())))
}

@Serializable
@AvroNamespace("io.infinitic.data")
data class WorkflowRequester(
  val workflowName: WorkflowName,
  val workflowVersion: WorkflowVersion?,
  val workflowId: WorkflowId,
  val workflowMethodName: MethodName,
  val workflowMethodId: WorkflowMethodId,
) : Requester {
  override fun toJson() = JsonObject(
      mapOf(
          "workflowName" to JsonPrimitive(workflowName.toString()),
          "workflowId" to JsonPrimitive(workflowId.toString()),
          "methodName" to JsonPrimitive(workflowMethodName.toString()),
          "methodId" to JsonPrimitive(workflowMethodId.toString()),
      ),
  )
}

val Requester?.clientName: ClientName?
  get() = when (this is ClientRequester) {
    true -> clientName
    false -> null
  }

val Requester?.workflowId: WorkflowId?
  get() = when (this is WorkflowRequester) {
    true -> workflowId
    false -> null
  }

val Requester?.workflowName: WorkflowName?
  get() = when (this is WorkflowRequester) {
    true -> workflowName
    false -> null
  }

val Requester?.workflowMethodName: MethodName?
  get() = when (this is WorkflowRequester) {
    true -> workflowMethodName
    false -> null
  }

val Requester?.workflowMethodId: WorkflowMethodId?
  get() = when (this is WorkflowRequester) {
    true -> workflowMethodId
    false -> null
  }

fun Requester.waitingClients(waitingClient: Boolean) = when (waitingClient) {
  false -> mutableSetOf()
  true -> when (this is ClientRequester) {
    true -> mutableSetOf(this.clientName)
    false -> thisShouldNotHappen()
  }
}
