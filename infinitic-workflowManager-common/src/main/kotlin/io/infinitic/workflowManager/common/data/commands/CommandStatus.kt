package io.infinitic.workflowManager.common.data.commands

import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskIndex

sealed class CommandStatus

data class CommandStatusOngoing(val unused: String? = null): CommandStatus()
data class CommandStatusCompleted(val result: Any?, val completionWorkflowTaskIndex: WorkflowTaskIndex): CommandStatus()
data class CommandStatusCanceled(val result: Any?, val cancellationWorkflowTaskIndex: WorkflowTaskIndex): CommandStatus()
