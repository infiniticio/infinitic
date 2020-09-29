package io.infinitic.common.workflows.data.commands

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.common.workflows.data.workflows.WorkflowMessageIndex

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "status")
@JsonSubTypes(
    JsonSubTypes.Type(value = CommandStatusOngoing::class, name = "ONGOING"),
    JsonSubTypes.Type(value = CommandStatusCompleted::class, name = "COMPLETED"),
    JsonSubTypes.Type(value = CommandStatusCanceled::class, name = "CANCELED")
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class CommandStatus

object CommandStatusOngoing : CommandStatus() {
    override fun equals(other: Any?) = javaClass == other?.javaClass
}

data class CommandStatusCompleted(
    @JsonProperty("result")
    val completionResult: CommandOutput,
    @JsonProperty("workflowMessageIndex")
    val completionWorkflowMessageIndex: WorkflowMessageIndex
) : CommandStatus()

data class CommandStatusCanceled(
    @JsonProperty("result")
    val cancellationResult: CommandOutput,
    @JsonProperty("workflowMessageIndex")
    val cancellationWorkflowMessageIndex: WorkflowMessageIndex
) : CommandStatus()
