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

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Requester

@Serializable
data class ClientRequester(
  val clientName: ClientName,
) : Requester

@Serializable
data class WorkflowRequester(
  val workflowName: WorkflowName,
  val workflowId: WorkflowId,
  val workflowMethodId: WorkflowMethodId,
) : Requester

val Requester.clientName: ClientName?
  get() = when (this is ClientRequester) {
    true -> clientName
    false -> null
  }

val Requester.workflowId: WorkflowId?
  get() = when (this is WorkflowRequester) {
    true -> workflowId
    false -> null
  }
