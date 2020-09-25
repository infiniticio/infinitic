package io.infinitic.common.workflowManager.data.methodRuns

import io.infinitic.common.workflowManager.data.commands.PastCommand
import io.infinitic.common.workflowManager.data.properties.Properties
import io.infinitic.common.workflowManager.data.steps.PastStep
import io.infinitic.common.workflowManager.data.workflows.WorkflowId
import io.infinitic.common.workflowManager.data.workflows.WorkflowMessageIndex

data class MethodRun(
    val methodRunId: MethodRunId = MethodRunId(),
    val isMain: Boolean,
    val parentWorkflowId: WorkflowId? = null,
    val parentMethodRunId: MethodRunId? = null,
    val methodName: MethodName,
    val methodInput: MethodInput,
    var methodOutput: MethodOutput? = null,
    val messageIndexAtStart: WorkflowMessageIndex,
    val propertiesAtStart: Properties = Properties(),
    val pastCommands: MutableList<PastCommand> = mutableListOf(),
    val pastSteps: MutableList<PastStep> = mutableListOf()
)