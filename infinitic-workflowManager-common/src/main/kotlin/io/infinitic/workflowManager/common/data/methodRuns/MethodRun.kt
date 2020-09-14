package io.infinitic.workflowManager.common.data.methodRuns

import io.infinitic.workflowManager.common.data.instructions.PastInstruction
import io.infinitic.workflowManager.common.data.properties.Properties

data class MethodRun(
    val methodRunId: MethodRunId,
    val methodName: MethodName,
    val methodInput: MethodInput,
    val propertiesAtMethodStart: Properties = Properties(),
    val pastInstructionsInMethod: List<PastInstruction> = listOf()
)
