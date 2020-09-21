package io.infinitic.workflowManager.common.data.steps

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.workflowManager.common.data.workflows.WorkflowMessageIndex

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "status")
@JsonSubTypes(
    JsonSubTypes.Type(value = StepStatusOngoing::class, name = "ONGOING"),
    JsonSubTypes.Type(value = StepStatusCompleted::class, name = "COMPLETED"),
    JsonSubTypes.Type(value = StepStatusCanceled::class, name = "CANCELED")
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class StepStatus

object StepStatusOngoing : StepStatus() {
    override fun equals(other: Any?) = javaClass == other?.javaClass
}

data class StepStatusCompleted(
    @JsonProperty("result")
    val completionResult: StepOutput,
    @JsonProperty("workflowMessageIndex")
    val completionWorkflowMessageIndex: WorkflowMessageIndex
) : StepStatus()

data class StepStatusCanceled(
    @JsonProperty("result")
    val cancellationResult: StepOutput,
    @JsonProperty("workflowMessageIndex")
    val cancellationWorkflowMessageIndex: WorkflowMessageIndex
) : StepStatus()
