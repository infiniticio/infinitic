package io.infinitic.workflowManager.common.exceptions

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
) : Exception("$msg.\n$help")

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
