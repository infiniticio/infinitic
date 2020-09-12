package io.infinitic.workflowManager.common.data.instructions

import io.infinitic.workflowManager.common.data.commands.NewCommand
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.workflowManager.common.data.workflows.WorkflowChangeCheckMode

abstract class PastInstruction(
    open val pastPosition: PastPosition
) {
    abstract fun isSimilarTo(newCommand: NewCommand, mode: WorkflowChangeCheckMode): Boolean
}
