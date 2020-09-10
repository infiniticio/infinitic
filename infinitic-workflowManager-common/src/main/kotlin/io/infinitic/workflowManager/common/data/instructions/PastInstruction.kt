package io.infinitic.workflowManager.common.data.instructions

import io.infinitic.workflowManager.common.data.commands.NewCommand
import io.infinitic.workflowManager.common.data.workflows.WorkflowIntegrityCheckMode

abstract class PastInstruction(val position: Position) {
    abstract fun isSimilarTo(newCommand: NewCommand, mode: WorkflowIntegrityCheckMode): Boolean
}
