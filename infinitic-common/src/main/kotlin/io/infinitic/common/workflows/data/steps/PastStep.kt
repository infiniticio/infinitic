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

package io.infinitic.common.workflows.data.steps

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.methodRuns.MethodRunPosition
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.steps.StepStatus.Canceled
import io.infinitic.common.workflows.data.steps.StepStatus.Completed
import io.infinitic.common.workflows.data.steps.StepStatus.CurrentlyFailed
import io.infinitic.common.workflows.data.steps.StepStatus.Failed
import io.infinitic.common.workflows.data.steps.StepStatus.Unknown
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import kotlinx.serialization.Serializable

@Serializable @AvroNamespace("io.infinitic.workflows.data")
data class PastStep(
    val stepPosition: MethodRunPosition,
    val step: Step,
    val stepHash: StepHash,
    val workflowTaskIndexAtStart: WorkflowTaskIndex,
    var stepStatus: StepStatus = StepStatus.Waiting,
    var propertiesNameHashAtTermination: Map<PropertyName, PropertyHash>? = null,
    var workflowTaskIndexAtTermination: WorkflowTaskIndex? = null
) {
    @JsonIgnore
    fun isTerminated() =
        stepStatus is Completed || stepStatus is Canceled || stepStatus is Failed || stepStatus is Unknown

    fun updateWith(pastCommand: PastCommand) {
        step.updateWith(pastCommand.commandId, pastCommand.commandStatus, pastCommand.commandStatuses)
        stepStatus = step.status()
    }

    fun isTerminatedBy(pastCommand: PastCommand): Boolean {
        // returns false if already terminated
        if (isTerminated()) return false

        val wasWaiting = stepStatus is StepStatus.Waiting

        // working on a copy to check without updating
        with(copy()) {
            // apply update
            updateWith(pastCommand)

            return isTerminated() || (stepStatus is CurrentlyFailed && wasWaiting)
        }
    }

    fun isSameThan(newStep: NewStep) = newStep.stepHash == stepHash
}
