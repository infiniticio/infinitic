// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.common.workflows.exceptions

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/*
 *  @JsonIgnoreProperties and @JsonProperty annotations are here
 *  to allow correct JSON ser/deserialization through constructors
 */

@JsonIgnoreProperties(ignoreUnknown = true)
sealed class UserException(
    open val msg: String,
    open val help: String
) : RuntimeException("$msg.\n$help") // Must be an unchecked exception, to avoid UndeclaredThrowableException when thrown from a proxy

sealed class UserExceptionInCommon(
    override val msg: String,
    override val help: String
) : UserException(msg, help)

sealed class UserExceptionInClient(
    override val msg: String,
    override val help: String
) : UserException(msg, help)

sealed class UserExceptionInWorker(
    override val msg: String,
    override val help: String
) : UserException(msg, help)

/***********************
 * Exceptions in common
 ***********************/

/***********************
 * Exceptions in client
 ***********************/

/***********************
 * Exceptions in worker
 ***********************/

data class WorkflowTaskContextNotInitialized(
    @JsonProperty("name") val name: String,
    @JsonProperty("context") val context: String
) : UserExceptionInWorker(
    msg = "\"context\" property not initialized in $name",
    help = "If you need to test your workflow, please initialize the context property by an instance of $context"
)

data class WorkflowDefinitionUpdatedWhileOngoing(
    @JsonProperty("workflowName") val workflowName: String,
    @JsonProperty("workflowMethodName") val workflowMethodName: String,
    @JsonProperty("position") val position: String
) : UserExceptionInWorker(
    msg = "Definition of workflow \"$workflowName\" has been updated since its launch (detected at position $position in $workflowMethodName)",
    help = "You can either kill this instance or revert its previous definition to be able to resume it"
)

data class NoMethodCallAtAsync(
    @JsonProperty("name") val name: String
) : UserExceptionInWorker(
    msg = "You must use a method of \"$name\" when using \"async\" method",
    help = "Make sure to call exactly one method of \"$name\" within the curly braces - example: async(foo) { bar(*args) }"
)

data class ShouldNotWaitInsideInlinedTask(
    @JsonProperty("method") val method: String,
) : UserExceptionInWorker(
    msg = "You must not suspend computations inside an inlined task",
    help = "In $method, make sure you do not wait for task or child workflow completion inside `task { ... }`"
)

data class ShouldNotUseAsyncFunctionInsideInlinedTask(
    @JsonProperty("method") val method: String,
) : UserExceptionInWorker(
    msg = "You must not suspend computations inside an inlined task",
    help = "In $method, make sure you do not use `async { ... }` function inside `task { ... }`"
)

data class WorkflowUsedAsTask(
    @JsonProperty("name") val name: String,
    @JsonProperty("workflow") val workflow: String
) : UserExceptionInWorker(
    msg = "$name is used as a task, but registered implementation $workflow is a workflow",
    help = "Check that you are using $name consistently between client and workers"
)

data class TaskUsedAsWorkflow(
    @JsonProperty("name") val name: String,
    @JsonProperty("task") val task: String
) : UserExceptionInWorker(
    msg = "$name is used as a workflow, but registered implementation $task is a task",
    help = "Check that you are using $name consistently between client and workers"
)
