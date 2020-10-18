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

package io.infinitic.common.workflows.data.methodRuns

import com.fasterxml.jackson.annotation.JsonIgnore
import io.infinitic.common.tasks.data.MethodInput
import io.infinitic.common.tasks.data.MethodName
import io.infinitic.common.tasks.data.MethodOutput
import io.infinitic.common.tasks.data.MethodParameterTypes
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.properties.PropertiesNameHash
import io.infinitic.common.workflows.data.properties.PropertiesHashValue
import io.infinitic.common.workflows.data.steps.PastStep
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMessageIndex
import java.lang.RuntimeException

data class MethodRun(
    val methodRunId: MethodRunId = MethodRunId(),
    val isMain: Boolean,
    val parentWorkflowId: WorkflowId? = null,
    val parentMethodRunId: MethodRunId? = null,
    val methodName: MethodName,
    val methodParameterTypes: MethodParameterTypes,
    val methodInput: MethodInput,
    var methodOutput: MethodOutput? = null,
    val messageIndexAtStart: WorkflowMessageIndex,
    val propertiesNameHashAtStart: PropertiesNameHash = PropertiesNameHash(),
    val pastCommands: MutableList<PastCommand> = mutableListOf(),
    val pastSteps: MutableList<PastStep> = mutableListOf()
) {
    @JsonIgnore
    fun getPropertiesNameValue(propertiesHashValue: PropertiesHashValue) = propertiesNameHashAtStart.mapValues {
            propertiesHashValue[it.value] ?: throw RuntimeException("Unknown hash ${it.value} in $propertiesHashValue")
        }
}
