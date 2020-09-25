package io.infinitic.common.workflowManager.data.commands

import com.fasterxml.jackson.annotation.JsonIgnore
import io.infinitic.common.workflowManager.data.methodRuns.MethodPosition
import io.infinitic.common.workflowManager.data.workflows.WorkflowChangeCheckMode

data class PastCommand(
    val commandPosition: MethodPosition,
    val commandType: CommandType,
    val commandId: CommandId,
    val commandHash: CommandHash,
    val commandSimpleName: CommandSimpleName,
    var commandStatus: CommandStatus
) {

    @JsonIgnore
    fun isTerminated() = this.commandStatus is CommandStatusCompleted || this.commandStatus is CommandStatusCanceled

    fun isSimilarTo(newCommand: NewCommand, mode: WorkflowChangeCheckMode): Boolean =
        newCommand.commandPosition == commandPosition &&
            when (mode) {
                WorkflowChangeCheckMode.NONE ->
                    true
                WorkflowChangeCheckMode.SIMPLE_NAME_ONLY ->
                    newCommand.commandType == commandType && newCommand.commandSimpleName == commandSimpleName
                WorkflowChangeCheckMode.ALL ->
                    newCommand.commandHash == commandHash
            }
}
