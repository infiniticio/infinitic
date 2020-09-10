package io.infinitic.workflowManager.worker

import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.workflows.WorkflowName
import io.infinitic.workflowManager.common.data.workflows.WorkflowOptions
import io.infinitic.workflowManager.worker.data.MethodContext
import io.infinitic.workflowManager.worker.exceptions.KnownStepException
import io.infinitic.workflowManager.worker.exceptions.NewStepException
import kotlinx.coroutines.runBlocking

fun main() = runBlocking<Unit> {

    val w = WorkflowAImpl()
    w.setMethodContext(
        MethodContext(
            workflowName = WorkflowName(""),
            workflowId = WorkflowId(),
            workflowOptions = WorkflowOptions(),
            pastInstructions = listOf()
        )
    )

    fun test() = try {
        val output = w.test1(1)
        println("output: $output")
    } catch (e: NewStepException) {
        println(e)
//        val pastInstructions = w.getBranchContext().pastInstructions + w.getBranchContext().newCommands.map {
//            println("newCommand: $it")
//            TaskDispatched(
//                taskId = TaskId(),
//                taskOutput = TaskOutput(null),
//                commandPosition = it.commandPosition,
//                commandHash = it.commandHash,
//                commandStatus = CommandStatus.DISPATCHED
//            )
//        }
//        w.setBranchContext(w.getBranchContext().copy(
//            pastInstructions = pastInstructions
//        ))
    } catch (e: KnownStepException) {
        println(e)

//        val pastInstructions = w.getBranchContext().pastInstructions.mapTo(mutableListOf()) {
//            when (it) {
//                is TaskDispatched -> it.copy(
//                    commandStatus = CommandStatus.COMPLETED,
//                    taskOutput = TaskOutput(1)
//                )
//                else -> it
//            }
//        }
//        w.setBranchContext(w.getBranchContext().copy(
//            pastInstructions = pastInstructions
//        ))
    } finally {
//        w.setBranchContext(w.getBranchContext().copy(
//            currentPosition = Position(null, -1),
//            newCommands = mutableListOf()
//        ))
//
//        println("context ${w.getBranchContext().pastInstructions}")
    }

    test()
//    test()
//    test()
//    test()
//    test()
//    test()
//    test()
//    test()
}
