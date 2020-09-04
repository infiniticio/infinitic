package io.infinitic.workflowManager.worker.data

import io.infinitic.workflowManager.common.data.commands.Command
import io.infinitic.workflowManager.common.data.commands.CommandHash
import io.infinitic.workflowManager.common.data.commands.CommandIndex
import io.infinitic.workflowManager.common.data.commands.HashedCommand
import io.infinitic.workflowManager.common.data.commands.PastCommand
import io.infinitic.workflowManager.common.data.steps.PastStep
import io.infinitic.workflowManager.common.data.steps.StepHash
import io.infinitic.workflowManager.common.data.steps.StepIndex
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.workflows.WorkflowName

data class BranchContext(
    val workflowName: WorkflowName,
    val workflowId: WorkflowId,
    val pastSteps: List<PastStep>,
    val pastCommands: List<PastCommand>,
    var currentStepIndex: StepIndex = StepIndex(-1),
    var currentCommandIndex: CommandIndex = CommandIndex(-1),
//    var newStep: Step? = null,
    val newCommands: MutableList<HashedCommand> = mutableListOf()
) {
     fun getMaxPastCommandIndex() = pastCommands
        .maxBy { it.commandIndex }
        ?.commandIndex
        ?: CommandIndex(-1)
}

