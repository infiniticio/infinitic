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

package io.infinitic.common.workflows.data.commands

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.errors.Error
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
sealed class CommandStatus {
    /**
     * A command is terminated if canceled or completed, failed is a transient state
     */
    @JsonIgnore fun isTerminated() = this is Completed || this is Canceled

    @Serializable
    @SerialName("CommandStatus.Running")
    object Running : CommandStatus() {
        override fun equals(other: Any?) = javaClass == other?.javaClass
        override fun toString(): String = Running::class.java.name
    }

    @Serializable
    @SerialName("CommandStatus.Completed")
    data class Completed(
        val returnValue: ReturnValue,
        val completionWorkflowTaskIndex: WorkflowTaskIndex
    ) : CommandStatus()

    @Serializable
    @SerialName("CommandStatus.Canceled")
    data class Canceled(
        val cancellationWorkflowTaskIndex: WorkflowTaskIndex
    ) : CommandStatus()

    @Serializable
    @SerialName("CommandStatus.CurrentlyFailed")
    data class CurrentlyFailed(
        val error: Error,
        val failureWorkflowTaskIndex: WorkflowTaskIndex
    ) : CommandStatus()
}
