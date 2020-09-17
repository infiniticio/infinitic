package io.infinitic.workflowManager.common.data.commands

import io.infinitic.workflowManager.common.data.workflows.WorkflowMessageIndex

sealed class CommandStatus

data class CommandStatusOngoing(val unused: String? = null) : CommandStatus()
data class CommandStatusCompleted(val result: Any?, val completionWorkflowMessageIndex: WorkflowMessageIndex) : CommandStatus()
data class CommandStatusCanceled(val result: Any?, val cancellationWorkflowMessageIndex: WorkflowMessageIndex) : CommandStatus()
