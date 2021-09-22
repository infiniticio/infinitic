/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.exceptions.clients

import io.infinitic.exceptions.UserException

sealed class ClientUserException(
    msg: String,
    help: String,
) : UserException("$msg.\n$help")

class InvalidStubException(
    klass: String? = null
) : ClientUserException(
    msg = when (klass) {
        null -> "Instance used"
        else -> "$klass is not the stub of a class or of a workflow"
    },
    help = "Make sure to use a stub returned by taskStub(Class<*>) or workflowStub(Class<*>)"
)

class InvalidChannelException(
    klass: String
) : ClientUserException(
    msg = "$klass is not the stub of a Channel",
    help = "Make sure to use something like `workflow.getchannel()` where `workflow` is the stub " +
        "of a workflow and `getChannel()` is a Channel getter"
)

class InvalidWorkflowException(
    klass: String
) : ClientUserException(
    msg = "$klass is not the stub of a workflow",
    help = "Make sure to use a stub returned by workflowStub(Class<*>)"
)

class InvalidInterfaceException(
    method: String
) : ClientUserException(
    msg = "$method must be a method declared from an interface",
    help = "Make sure to provide a method defined from an interface, not from an actual instance."
)

class SuspendMethodNotSupportedException(
    klass: String,
    method: String
) : ClientUserException(
    msg = "Method \"$klass:$method\" is a suspend function",
    help = "Suspend functions are not supported yet"
)

class InvalidChannelUsageException() : ClientUserException(
    msg = "send method of channels can not be used directly",
    help = "Make sure to use the send(workflow, id) function"
)
