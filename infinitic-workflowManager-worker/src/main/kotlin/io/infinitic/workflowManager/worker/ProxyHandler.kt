package io.infinitic.workflowManager.worker

import io.infinitic.common.data.interfaces.inc
import io.infinitic.common.data.interfaces.plus
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.workflowManager.common.data.commands.DispatchTask
import io.infinitic.workflowManager.common.data.commands.HashedCommand
import io.infinitic.workflowManager.common.data.commands.TaskDispatched
import io.infinitic.workflowManager.common.exceptions.WorkflowUpdatedWhileRunning
import io.infinitic.workflowManager.data.commands.CommandStatus
import io.infinitic.workflowManager.worker.data.BranchContext
import io.infinitic.workflowManager.worker.exceptions.KnownStepException
import io.infinitic.workflowManager.worker.exceptions.NewStepException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class ProxyHandler(val getBranchContext: () -> BranchContext) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any?  {
        val branchContext = getBranchContext()
        // increment current command index
        val currentCommandIndex = branchContext.currentCommandIndex++
        // get max command index in past commands
        val maxPastCommandIndex = branchContext.getMaxPastCommandIndex()
        // set current command
        val currentCommand = DispatchTask(
            commandIndex = branchContext.currentCommandIndex,
            taskName = TaskName.from(method),
            taskInput = TaskInput(args ?: arrayOf<Any>())
        )
        val currentCommandHash = currentCommand.hash()
        // commands
//        println("max: $maxPastCommandIndex")
        when {
            // this command is already known
            currentCommandIndex <= maxPastCommandIndex -> {
//                println("known: $currentCommandIndex")
                val pastCommand = branchContext.pastCommands.find { it.commandIndex == currentCommandIndex }!!
                // checking this is the same command than previously
                if (pastCommand !is TaskDispatched || pastCommand.commandHash != currentCommandHash) {
                    // hopefully it's not a bug but an error by the user
                    throw WorkflowUpdatedWhileRunning(branchContext.workflowName.name, method.name, currentCommandIndex.int)
                }
                // this command is already known => do nothing
                when(pastCommand.commandStatus) {
                    CommandStatus.DISPATCHED -> throw KnownStepException()
                    CommandStatus.CANCELED, CommandStatus.COMPLETED -> {
                        // set new properties
                        // TODO("Set workflow properties after task completion")
                        // return result
                        return pastCommand.taskOutput!!.data
                    }
                }
            }
            // this is a new command, we add it to the newCommands list
            currentCommandIndex == maxPastCommandIndex + 1 -> {
//                println("new: $currentCommandIndex")
                branchContext.newCommands.add(HashedCommand(
                    command = currentCommand,
                    commandIndex = currentCommandIndex,
                    commandHash = currentCommandHash
                ))
                throw NewStepException()
            }
            // hopefully it's not a bug but an error by the user
            else -> {
//                println("error: $currentCommandIndex")
                throw WorkflowUpdatedWhileRunning(branchContext.workflowName.name, method.name, currentCommandIndex.int)
            }
        }
    }
}
