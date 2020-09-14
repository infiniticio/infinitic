package io.infinitic.workflowManager.worker

import io.infinitic.workflowManager.common.data.instructions.PastInstruction
import io.infinitic.workflowManager.common.data.methodRuns.MethodName
import io.infinitic.workflowManager.common.data.methodRuns.MethodRunId
import io.infinitic.workflowManager.common.data.methodRuns.MethodInput
import io.infinitic.workflowManager.common.data.methodRuns.MethodRun
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.properties.PropertyHash
import io.infinitic.workflowManager.common.data.properties.PropertyName
import io.infinitic.workflowManager.common.data.properties.PropertyStore
import io.infinitic.workflowManager.common.data.properties.PropertyValue
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskInput
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.workflows.WorkflowName
import io.infinitic.workflowManager.common.data.workflows.WorkflowOptions
import io.infinitic.workflowManager.worker.deferred.DeferredIdd
import kotlinx.coroutines.runBlocking

fun main() = runBlocking<Unit> {

    val d1 = DeferredIdd<String>("qwerty")
    val d2 = DeferredIdd<String>("asdfgh")

    val methodRun = MethodRun(
        methodRunId = MethodRunId(),
        methodName = MethodName.from(WorkflowA::class.java.methods.first { it.name == "test1" }),
        methodInput = MethodInput(0),
        methodPropertiesAtStart = Properties(mapOf<PropertyName, PropertyHash>()),
        methodPastInstructions = listOf<PastInstruction>()
    )

    val input = WorkflowTaskInput(
        workflowId = WorkflowId(),
        workflowName = WorkflowName(WorkflowAImpl::class.java.name),
        workflowOptions = WorkflowOptions(),
        workflowPropertyStore = PropertyStore(mutableMapOf<PropertyHash, PropertyValue>()),
        workflowTaskIndex = WorkflowTaskIndex(0),
        methodRun = methodRun
    )

//    WorkflowTaskImpl().handle(input)

//    fun test() = try {
//        val output = w.test1(1)
//        println("output: $output")
//    } catch (e: NewStepException) {
//        println(e)
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
//    } catch (e: KnownStepException) {
//        println(e)

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
//    } finally {
//        w.setBranchContext(w.getBranchContext().copy(
//            currentPosition = Position(null, -1),
//            newCommands = mutableListOf()
//        ))
//
//        println("context ${w.getBranchContext().pastInstructions}")
//    }

//    test()
//    test()
//    test()
//    test()
//    test()
//    test()
//    test()
//    test()
}
