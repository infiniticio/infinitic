package io.infinitic.workflowManager.common.data.steps

import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskIndex

sealed class StepStatus

data class StepStatusOngoing(val unused: String? = null): StepStatus()
data class StepStatusCompleted(val result: Any?, val completionWorkflowTaskIndex: WorkflowTaskIndex): StepStatus()
data class StepStatusCanceled(val result: Any?, val cancellationWorkflowTaskIndex: WorkflowTaskIndex): StepStatus()

