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

class InvalidNewStubException(
    klass: String,
    action: String
) : ClientUserException(
    msg = "The provided instance of $klass is not the stub of a class or of a workflow",
    help = "In InfiniticClient::$action, make sure to use a stub returned by InfiniticClient::newTaskStub or InfiniticClient::newWorkflowStub"
)

class InvalidInstanceStubException(
    klass: String,
    action: String
) : ClientUserException(
    msg = "The provided instance of $klass is not the stub of a class or of a workflow",
    help = "In InfiniticClient::$action, make sure to use a stub returned by InfiniticClient::getInstanceStub "
)

class InvalidInterfaceException(
    method: String,
    action: String
) : ClientUserException(
    msg = "When using InfiniticClient::$action, $method must be a method declared from an interface",
    help = "Make sure to provide a method defined from an interface, not from an actual instance."
)

class DispatchTaskSelectionException(
    name: String,
    action: String
) : ClientUserException(
    msg = "First parameter of InfiniticClient::$action can not be the stub of existing task",
    help = "Make sure to use the stub returned by InfiniticClient::newTaskStub($name)"
)

class SuspendMethodNotSupportedException(
    klass: String,
    method: String
) : ClientUserException(
    msg = "Method \"$klass:$method\" is a suspend function",
    help = "Suspend functions are not supported yet"
)

class UseChannelOnNewWorkflowException(
    workflow: String
) : ClientUserException(
    msg = "Channels can only be used with an existing instance of $workflow workflow",
    help = "Make sure to target a running workflow InfiniticClient::getInstanceStub"
)

class CanNotAwaitStubPerTag(
    klass: String
) : ClientUserException(
    msg = "You can not await a task or workflow ($klass) based on a tag",
    help = "Please target the task or workflow per id"
)
