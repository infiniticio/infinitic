package io.infinitic.workflowManager.worker.data

import io.infinitic.workflowManager.common.data.commands.Command
import io.infinitic.workflowManager.common.data.steps.BlockingStep

data class WorkflowTaskContext(
    val blockingSteps: List<BlockingStep>,
    val commands: List<Command>
)
