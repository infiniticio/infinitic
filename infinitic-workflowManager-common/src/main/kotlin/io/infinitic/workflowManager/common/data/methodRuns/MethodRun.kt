package io.infinitic.workflowManager.common.data.methodRuns

import io.infinitic.workflowManager.common.data.instructions.PastInstruction
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.workflows.WorkflowId

data class MethodRun(
    val parentWorkflowId: WorkflowId? = null,
    val parentMethodRunId: MethodRunId? = null,
    val methodRunId: MethodRunId = MethodRunId(),
    val methodName: MethodName,
    val methodInput: MethodInput,
    var methodOutput: MethodOutput? = null,
    val propertiesAtMethodStart: Properties = Properties(),
    val pastInstructionsInMethod: List<PastInstruction> = listOf()
)
