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

data class WorkflowUpdatedWhileRunning(
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

data class ShouldNotWaitInInlineTask(
    @JsonProperty("unused") val unused: String = ""
) : UserExceptionInWorker(
    msg = "You can not suspend computations inside an inline task",
    help = "Make sure you do not wait in your inline task"
)
