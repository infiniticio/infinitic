package io.infinitic.workflowManager.worker

import io.infinitic.common.data.DateTime
import io.infinitic.common.data.interfaces.plus
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskOutput
import io.infinitic.workflowManager.common.data.commands.CommandIndex
import io.infinitic.workflowManager.common.data.commands.PastCommand
import io.infinitic.workflowManager.common.data.commands.TaskDispatched
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.workflows.WorkflowName
import io.infinitic.workflowManager.data.commands.CommandStatus
import io.infinitic.workflowManager.worker.data.BranchContext
import io.infinitic.workflowManager.worker.exceptions.KnownStepException
import io.infinitic.workflowManager.worker.exceptions.NewStepException
import kotlinx.coroutines.runBlocking

fun main() = runBlocking<Unit> {
    val w = WorkflowAImpl()
    w.branchContext = BranchContext(
        workflowName = WorkflowName(""),
        workflowId = WorkflowId(),
        pastSteps = listOf(),
        pastCommands = listOf()
    )

    fun test() = try {
        val output = w.test1(1)
        println("output: $output")
    } catch(e: NewStepException) {
        val pastCommands = w.branchContext.pastCommands + w.branchContext.newCommands.map {
            println("newCommand: $it")
            TaskDispatched(
                taskId = TaskId(),
                taskOutput = TaskOutput(null),
                commandIndex = it.commandIndex,
                commandHash = it.commandHash,
                commandStatus = CommandStatus.DISPATCHED,
                decidedAt = DateTime()
            )
        }
        w.branchContext = w.branchContext.copy(
            pastCommands = pastCommands
        )
    } catch(e: KnownStepException) {
        val pastCommands = w.branchContext.pastCommands.mapTo(mutableListOf()) {
            when(it) {
                is TaskDispatched -> it.copy(
                    commandStatus = CommandStatus.COMPLETED,
                    taskOutput = TaskOutput(1)
                )
                else -> it
            }
        }
        w.branchContext = w.branchContext.copy(
            pastCommands = pastCommands
        )
    } finally {
        w.branchContext = w.branchContext.copy(
            currentCommandIndex = CommandIndex(-1),
            newCommands = mutableListOf()
        )

        println("context ${w.branchContext.pastCommands}")
    }

    test()
    test()
    test()
    test()
    test()
    test()
    test()
    test()
}
