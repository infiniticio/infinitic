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
import io.infinitic.workflows.SendChannel
import kotlinx.serialization.Serializable

@Serializable
sealed class ClientException(
    val msg: String,
    val help: String
) : UserException("$msg.\n$help")

@Serializable
data class NotAStubException(
    val name: String,
    val async: String
) : ClientException(
    msg = "The first parameter of the client.$async(.) function should be a stub",
    help = "Make sure to provide the stub provided by a client.task($name) or client.workflow($name) function."
)

@Serializable
data class IncorrectExistingStubException(
    val name: String,
    val action: String
) : ClientException(
    msg = "The first parameter of the client.$action(.) function should be the stub of an existing task or workflow",
    help = "Make sure to provide the stub returned by client.getTask or client.getWorkflow functions"
)

@Serializable
data class CanNotReuseWorkflowStubException(
    val name: String
) : ClientException(
    msg = "You can not reuse a workflow stub ($name) already dispatched",
    help = "Please create a new stub using `newWorkflow()` for each workflow dispatch`"
)

@Serializable
data class CanNotApplyOnNewTaskStubException(
    val name: String,
    val action: String
) : ClientException(
    msg = "You can not `$action` a new task stub",
    help = "Please target an existing $name task using `getTask()` "
)

@Serializable
data class CanNotApplyOnNewWorkflowStubException(
    val name: String,
    val action: String
) : ClientException(
    msg = "You can not `$action` a new workflow stub",
    help = "Please target an existing $name workflow using `getWorkflow()` "
)

@Serializable
data class SuspendMethodNotSupportedException(
    val klass: String,
    val method: String
) : ClientException(
    msg = "method \"$method\" in class \"$klass\" is a suspend function",
    help = "Suspend functions are not supported"
)

@Serializable
data class NoMethodCallException(
    val klass: String
) : ClientException(
    msg = "The method to call for your task or workflow is missing",
    help = "Make sure to call a method of \"$klass\" in the second parameter of client.async()"
)

@Serializable
data class NoSendMethodCallException(
    val klass: String,
    val channel: String
) : ClientException(
    msg = "Nothing to send for $klass::$channel",
    help = "Make sure to call the \"send\" method in the second parameter of client.async($channel())"
)

@Serializable
data class MultipleMethodCallsException(
    val klass: String,
    val method1: String,
    val method2: String
) : ClientException(
    msg = "Only one method of \"$klass\" can be dispatched at a time. You can not call \"$method2\" method as you have already called \"$method1\"",
    help = "Make sure you call only one method of \"$klass\" - multiple calls in the provided lambda is forbidden"
)

@Serializable
data class ChannelUsedOnNewWorkflowException(
    val workflow: String
) : ClientException(
    msg = "Channels can only be used for an existing instance of $workflow workflow",
    help = "Make sure you target a running workflow, by providing and id when defining your workflow stub"
)

@Serializable
data class UnknownMethodInSendChannelException(
    val workflow: String,
    val channel: String,
    val method: String
) : ClientException(
    msg = "Unknown method $method used on channel $channel in $workflow",
    help = "Make sure to use the ${SendChannel<*>::send.name} method"
)

@Serializable
data class UnknownTaskException(
    val taskId: String,
    val taskName: String,
) : ClientException(
    msg = "Failed to  wait for task $taskId completion ($taskName)",
    help = "This task instance is probably already completed"
)

@Serializable
data class CanceledTaskException(
    val taskId: String,
    val taskName: String,
) : ClientException(
    msg = "task $taskId ($taskName) has been canceled",
    help = ""
)

@Serializable
data class UnknownWorkflowException(
    val workflowId: String,
    val workflowName: String
) : ClientException(
    msg = "Failed to  wait for workflow $workflowId completion ($workflowName)",
    help = "This workflow instance is probably already completed"
)

@Serializable
data class CanceledWorkflowException(
    val workflowId: String,
    val workflowName: String
) : ClientException(
    msg = "workflow $workflowId ($workflowName) has been canceled",
    help = ""
)
