package io.infinitic.workflowManager.common.data.methods

import io.infinitic.workflowManager.common.data.instructions.PastInstruction
import io.infinitic.workflowManager.common.data.properties.Properties

data class MethodRun(
    val methodId: MethodId,
    val methodName: MethodName,
    val methodInput: MethodInput,
    val methodPropertiesAtStart: Properties,
    val methodPastInstructions: List<PastInstruction>
)
