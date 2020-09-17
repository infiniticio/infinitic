package io.infinitic.workflowManager.common.data.steps

import io.infinitic.workflowManager.common.data.workflows.WorkflowMessageIndex

sealed class StepStatus

data class StepStatusOngoing(val unused: String? = null) : StepStatus()
data class StepStatusCompleted(val result: Any?, val completionWorkflowMessageIndex: WorkflowMessageIndex) : StepStatus()
data class StepStatusCanceled(val result: Any?, val cancellationWorkflowMessageIndex: WorkflowMessageIndex) : StepStatus()
