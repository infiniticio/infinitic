package io.infinitic.workflowManager.common.data.commands

import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowEventIndex

sealed class CommandStatus

data class CommandStatusOngoing(val unused: String? = null) : CommandStatus()
data class CommandStatusCompleted(val result: Any?, val completionWorkflowEventIndex: WorkflowEventIndex) : CommandStatus()
data class CommandStatusCanceled(val result: Any?, val cancellationWorkflowEventIndex: WorkflowEventIndex) : CommandStatus()
