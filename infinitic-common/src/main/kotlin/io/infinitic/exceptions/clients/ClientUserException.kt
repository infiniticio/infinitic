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
package io.infinitic.exceptions.clients

import io.infinitic.exceptions.UserException
import io.infinitic.workflows.SendChannel

sealed class ClientUserException(msg: String, help: String) : UserException("$msg.\n$help")

class InvalidStubException(klass: String? = null) :
  ClientUserException(
      msg =
      when (klass) {
        null -> "Instance used"
        else -> "$klass is not the stub of a class or of a workflow"
      },
      help = "Make sure to use a stub returned by 'newTask(Class<*>)' or 'newWorkflow(Class<*>)'",
  )

class InvalidIdTagSelectionException(klass: String? = null) :
  ClientUserException(
      msg =
      when (klass) {
        null -> "Instance used"
        else -> "$klass is not the stub of workflows selected by tag"
      },
      help = "Make sure to get your stub from the 'getWorkflowByTag' method",
  )

class InvalidChannelGetterException(klass: String) :
  ClientUserException(
      msg = "Invalid channel getter",
      help =
      "When defining getters of channels in your workflow interface, " +
          "make sure to have '${SendChannel::class.java.name}' as return type, not $klass",
  )

class InvalidRunningTaskException(klass: String) :
  ClientUserException(
      msg = "$klass is not the stub of a running task",
      help = "Make sure to use a stub returned by 'newTask(Class<*>)'",
  )

data object MultipleCustomIdException : ClientUserException(msg = "", help = "") {
  private fun readResolve(): Any = MultipleCustomIdException
}

class InvalidChannelUsageException :
  ClientUserException(
      msg = "send method of channels can not be used directly",
      help = "Make sure to send signals from the stub channel of an existing workflow 'getWorkflowById(...).myChannel.send(...)'",
  )

