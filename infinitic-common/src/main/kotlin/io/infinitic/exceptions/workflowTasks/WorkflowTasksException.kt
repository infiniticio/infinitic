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

package io.infinitic.exceptions.workflowTasks

import io.infinitic.exceptions.UserException
import io.infinitic.workflows.Channel

sealed class WorkflowTaskException(
    msg: String,
    help: String
) : UserException("$msg.\n$help")

class WorkflowUpdatedWhileRunningException(
    workflow: String,
    method: String,
    position: String
) : WorkflowTaskException(
    msg = "Workflow \"$workflow\" has been updated since its launch (detected at position $position in $method)",
    help = "You can either kill this instance or restore the workflow definition to be able to resume it"
)

class NoMethodCallAtAsyncException(
    klass: String
) : WorkflowTaskException(
    msg = "You must use a method of \"$klass\" when using \"async\" method",
    help = "Make sure to call exactly one method of \"$klass\" within the curly braces - example: async(foo) { bar(*args) }"
)

class MultipleMethodCallsAtAsyncException(
    klass: String,
    method1: String?,
    method2: String
) : WorkflowTaskException(
    msg = "Only one method of \"$klass\" can be called at a time. You can not call \"$method2\" method as you have already called \"$method1\"",
    help = "Make sure you call only one method of \"$klass\" - multiple calls in the provided lambda is forbidden"
)

class ShouldNotWaitInsideInlinedTaskException(
    method: String
) : WorkflowTaskException(
    msg = "Asynchronous computation inside an inlined task in forbidden",
    help = "In $method, make sure you do not wait for task or child workflow completion inside `inline { ... }`"
)

class ShouldNotUseAsyncFunctionInsideInlinedTaskException(
    method: String
) : WorkflowTaskException(
    msg = "Asynchronous computation inside an inlined task in forbidden",
    help = "In $method, make sure you do not use `async { ... }` function inside `task { ... }`"
)

class ParametersInChannelMethodException(
    workflow: String,
    method: String
) : WorkflowTaskException(
    msg = "in workflow $workflow, method $method returning a ${Channel::class.simpleName} should NOT have any parameter",
    help = ""
)

class NonUniqueChannelFromChannelMethodException(
    workflow: String,
    method: String
) : WorkflowTaskException(
    msg = "in workflow $workflow, method $method should return the same ${Channel::class.simpleName} instance when called multiple times",
    help = ""
)

class MultipleNamesForChannelException(
    workflow: String,
    method: String,
    otherMethod: String
) : WorkflowTaskException(
    msg = "in workflow $workflow, method $method return a ${Channel::class.simpleName} instance already associated with name $otherMethod",
    help = "Make sure to not have multiple methods returning the same channel"
)

object NameNotInitializedInChannelException : WorkflowTaskException(
    msg = "A ${Channel::class.simpleName} is used without name",
    help = "Make sure to have a method that returns this channel."
)
