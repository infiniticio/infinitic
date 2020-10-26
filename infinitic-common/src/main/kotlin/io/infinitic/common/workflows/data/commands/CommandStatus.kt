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

package io.infinitic.common.workflows.data.commands

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex

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
    val completionWorkflowTaskIndex: WorkflowTaskIndex
) : CommandStatus()

data class CommandStatusCanceled(
    @JsonProperty("result")
    val cancellationResult: CommandOutput,
    @JsonProperty("workflowMessageIndex")
    val cancellationWorkflowTaskIndex: WorkflowTaskIndex
) : CommandStatus()
