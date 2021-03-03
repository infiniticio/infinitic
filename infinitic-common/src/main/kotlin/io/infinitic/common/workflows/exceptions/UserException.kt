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

package io.infinitic.common.workflows.exceptions

import io.infinitic.workflows.Channel
import kotlinx.serialization.Serializable

@Serializable
sealed class UserException : RuntimeException()

@Serializable
sealed class UserExceptionInWorker(
    val msg: String,
    val help: String
) : UserException() {
    override val message = "$msg.\n$help"
}

/***********************
 * Exceptions in common
 ***********************/

/***********************
 * Exceptions in client
 ***********************/

/***********************
 * Exceptions in worker
 ***********************/

@Serializable
data class WorkflowUpdatedWhileRunning(
    val workflowName: String,
    val workflowMethodName: String,
    val position: String
) : UserExceptionInWorker(
    msg = "Definition of workflow \"$workflowName\" has been updated since its launch (detected at position $position in $workflowMethodName)",
    help = "You can either kill this instance or revert its previous definition to be able to resume it"
)

@Serializable
data class NoMethodCallAtAsync(
    val name: String
) : UserExceptionInWorker(
    msg = "You must use a method of \"$name\" when using \"async\" method",
    help = "Make sure to call exactly one method of \"$name\" within the curly braces - example: async(foo) { bar(*args) }"
)

@Serializable
data class ShouldNotWaitInsideInlinedTask(
    val method: String
) : UserExceptionInWorker(
    msg = "You must not suspend computations inside an inlined task",
    help = "In $method, make sure you do not wait for task or child workflow completion inside `task { ... }`"
)

@Serializable
data class ShouldNotUseAsyncFunctionInsideInlinedTask(
    val method: String
) : UserExceptionInWorker(
    msg = "You must not suspend computations inside an inlined task",
    help = "In $method, make sure you do not use `async { ... }` function inside `task { ... }`"
)

@Serializable
data class WorkflowUsedAsTask(
    val name: String,
    val workflow: String
) : UserExceptionInWorker(
    msg = "$name is used as a task, but registered implementation $workflow is a workflow",
    help = "Check that you are using $name consistently between client and workers"
)

@Serializable
data class TaskUsedAsWorkflow(
    val name: String,
    val task: String
) : UserExceptionInWorker(
    msg = "$name is used as a workflow, but registered implementation $task is a task",
    help = "Check that you are using $name consistently between client and workers"
)

@Serializable
data class ParametersInChannelMethod(
    val workflowName: String,
    val methodName: String
) : UserExceptionInWorker(
    msg = "in workflow $workflowName, method $methodName returning a ${Channel::class.simpleName} should NOT have any parameter",
    help = ""
)

@Serializable
data class NonUniqueChannelFromChannelMethod(
    val workflowName: String,
    val methodName: String
) : UserExceptionInWorker(
    msg = "in workflow $workflowName, method $methodName should return the same ${Channel::class.simpleName} instance when called multiple times",
    help = ""
)

@Serializable
data class MultipleNamesForChannel(
    val workflowName: String,
    val methodName: String,
    val otherName: String
) : UserExceptionInWorker(
    msg = "in workflow $workflowName, method $methodName return a ${Channel::class.simpleName} instance already associated with name $otherName",
    help = "Make sure to not have multiple methods returning the same channel"
)

@Serializable
object NameNotInitializedInChannel : UserExceptionInWorker(
    msg = "A ${Channel::class.simpleName} is used without name",
    help = "Make sure to have a method that returns this channel."
)
