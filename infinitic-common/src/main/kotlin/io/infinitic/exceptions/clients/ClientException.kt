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

sealed class ClientException(
    msg: String,
    help: String
) : UserException("$msg.\n$help")

class NotAStubException(
    name: String,
    async: String
) : ClientException(
    msg = "First parameter of client.$async function should be a stub",
    help = "Make sure to provide the stub returned by client.newTask($name) or client.newWorkflow($name) function"
)

class CanNotApplyOnChannelException(
    action: String
) : ClientException(
    msg = "First parameter of client.$action should be the stub of an existing task or workflow",
    help = "Make sure to provide the stub returned by client.getTask or client.getWorkflow function"
)

class CanNotReuseWorkflowStubException(
    name: String
) : ClientException(
    msg = "You can not reuse a workflow stub ($name) already dispatched",
    help = "Please create a new stub using `newWorkflow()` for each workflow dispatch`"
)

class CanNotApplyOnNewTaskStubException(
    name: String,
    action: String
) : ClientException(
    msg = "You can not `$action` on the stub of a new task",
    help = "Please target an existing $name task using `client.getTask()` "
)

class CanNotApplyOnNewWorkflowStubException(
    name: String,
    action: String
) : ClientException(
    msg = "You can not `$action` on the stub of a new workflow",
    help = "Please target an existing $name workflow using `client.getWorkflow()` "
)

class SuspendMethodNotSupportedException(
    klass: String,
    method: String
) : ClientException(
    msg = "method \"$method\" in class \"$klass\" is a suspend function",
    help = "Suspend functions are not supporteds"
)

class NoMethodCallException(
    klass: String
) : ClientException(
    msg = "The method to call for your task or workflow is missing",
    help = "Make sure to call a method of \"$klass\" in the second parameter of client.async()"
)

class NoSendMethodCallException(
    klass: String,
    channel: String
) : ClientException(
    msg = "Nothing to send for $klass::$channel",
    help = "Make sure to call the \"send\" method in the second parameter of client.async($channel())"
)

class MultipleMethodCallsException(
    klass: String,
    method1: String,
    method2: String
) : ClientException(
    msg = "Only one method of \"$klass\" can be dispatched at a time. You can not call \"$method2\" method as you have already called \"$method1\"",
    help = "Make sure you call only one method of \"$klass\" - multiple calls in the provided lambda is forbidden"
)

class ChannelUsedOnNewWorkflowException(
    workflow: String
) : ClientException(
    msg = "Channels can only be used for an existing instance of $workflow workflow",
    help = "Make sure you target a running workflow, by providing and id when defining your workflow stub"
)

class UnknownMethodInSendChannelException(
    workflow: String,
    channel: String,
    method: String
) : ClientException(
    msg = "Unknown method $method used on channel $channel in $workflow",
    help = "Make sure to use the ${SendChannel<*>::send.name} method"
)

class UnknownTaskException(
    taskId: String,
    taskName: String,
) : ClientException(
    msg = "Unknown workflow instance: $taskName ($taskId)",
    help = "This instance is probably already completed"
)

class CanceledTaskException(
    taskId: String,
    taskName: String,
) : ClientException(
    msg = "task canceled: $taskName ($taskId)",
    help = ""
)

class FailedTaskException(
    taskId: String,
    taskName: String,
    errorStackTrace: String
) : ClientException(
    msg = "task instance failed: $taskName ($taskId)",
    help = errorStackTrace
)

class UnknownWorkflowException(
    workflowId: String,
    workflowName: String
) : ClientException(
    msg = "Unknown workflow instance: $workflowName ($workflowId)",
    help = "This instance is probably already completed"
)

class CanceledWorkflowException(
    workflowId: String,
    workflowName: String
) : ClientException(
    msg = "workflow instance canceled: $workflowName ($workflowId)",
    help = ""
)

class AlreadyCompletedWorkflowException(
    workflowId: String,
    workflowName: String
) : ClientException(
    msg = "workflow instance's main method already completed: $workflowName ($workflowId)",
    help = ""
)

class FailedWorkflowException(
    workflowId: String,
    workflowName: String,
    errorStackTrace: String
) : ClientException(
    msg = "workflow instance failed: $workflowName ($workflowId)",
    help = errorStackTrace
)
