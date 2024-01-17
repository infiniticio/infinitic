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

package io.infinitic.events

import io.cloudevents.CloudEvent

class InfiniticEventImpl(ce: CloudEvent) : InfiniticEvent, CloudEvent by ce {
  override val isTaskEvent: Boolean
    get() = TODO("Not yet implemented")
  override val isWorkflowEvent: Boolean
    get() = TODO("Not yet implemented")
  override val serviceName: String?
    get() = TODO("Not yet implemented")
  override val taskId: String?
    get() = TODO("Not yet implemented")
  override val workflowName: String?
    get() = TODO("Not yet implemented")
  override val workflowId: String?
    get() = TODO("Not yet implemented")

  companion object {
    internal const val SCHEMAS_FOLDER = "schemas"

    internal const val SCHEMA_PREFIX = "https://raw.githubusercontent.com/" +
        "infiniticio/infinitic/main/infinitic-logs/src/main/resources/$SCHEMAS_FOLDER/"

    internal const val SCHEMA_EXTENSION = ".avsc"
  }
}

interface InfiniticEvent {
  val isTaskEvent: Boolean
  val isWorkflowEvent: Boolean
  val serviceName: String?
  val taskId: String?
  val workflowName: String?
  val workflowId: String?
}
