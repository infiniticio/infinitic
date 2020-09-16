package io.infinitic.workflowManager.common.data.steps

import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowEventIndex

sealed class StepStatus

data class StepStatusOngoing(val unused: String? = null) : StepStatus()
data class StepStatusCompleted(val result: Any?, val completionWorkflowEventIndex: WorkflowEventIndex) : StepStatus()
data class StepStatusCanceled(val result: Any?, val cancellationWorkflowEventIndex: WorkflowEventIndex) : StepStatus()
