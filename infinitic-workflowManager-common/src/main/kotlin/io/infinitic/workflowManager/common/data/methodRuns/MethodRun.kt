package io.infinitic.workflowManager.common.data.methodRuns

import io.infinitic.workflowManager.common.data.commands.PastCommand
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.steps.PastStep
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.workflows.WorkflowMessageIndex

data class MethodRun(
    val isMain: Boolean,
    val parentWorkflowId: WorkflowId? = null,
    val methodRunId: MethodRunId = MethodRunId(),
    val methodName: MethodName,
    val methodInput: MethodInput,
    var methodOutput: MethodOutput? = null,
    val messageIndexAtStart: WorkflowMessageIndex,
    val propertiesAtStart: Properties = Properties(),
    val pastCommands: MutableList<PastCommand> = mutableListOf(),
    val pastSteps: MutableList<PastStep> = mutableListOf()
)
