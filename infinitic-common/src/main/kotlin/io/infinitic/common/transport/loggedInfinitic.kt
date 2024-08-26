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
package io.infinitic.common.transport

import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.messages.interfaces.MethodMessage
import io.infinitic.common.clients.messages.interfaces.TaskMessage
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.messages.Message
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.events.messages.ServiceExecutorEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.tasks.tags.messages.WithTagAsKey
import io.infinitic.common.tasks.tags.messages.WithTaskIdAsKey
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.WorkflowMessageInterface
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage

internal fun Message.id() = when (this) {
  is WorkflowMessageInterface -> workflowId
  is ClientMessage -> when (this) {
    is MethodMessage -> workflowId
    is TaskMessage -> taskId
    else -> null
  }

  is ServiceExecutorMessage -> when (isWorkflowTask()) {
    true -> (requester as WorkflowRequester).workflowId
    false -> taskId
  }

  is ServiceExecutorEventMessage -> when (isWorkflowTask()) {
    true -> (requester as WorkflowRequester).workflowId
    false -> taskId
  }

  is WorkflowTagEngineMessage -> workflowTag

  is ServiceTagMessage -> when (this) {
    is WithTaskIdAsKey -> taskId
    is WithTagAsKey -> taskTag
  }

  else -> null
}

fun formatLog(id: Any?, command: String, message: Any? = null) =
    id.withPrefix + command.align + (message?.toString() ?: "")

private val String.align get() = this.padEnd(11) + " "

private val Any?.withPrefix
  get() = this?.let {
    when (it) {
      is WorkflowId -> "WID"
      is TaskId -> "TID"
      is TaskTag, is WorkflowTag -> "TAG"
      else -> thisShouldNotHappen()
    } + " $it - "
  } ?: ""
