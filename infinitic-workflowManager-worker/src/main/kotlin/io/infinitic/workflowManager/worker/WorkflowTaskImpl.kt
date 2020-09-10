package io.infinitic.workflowManager.worker

import io.infinitic.taskManager.common.parser.getMethodPerNameAndParameterCount
import io.infinitic.taskManager.common.parser.getMethodPerNameAndParameterTypes
import io.infinitic.taskManager.common.parser.getNewInstancePerName
import io.infinitic.workflowManager.common.data.steps.Step
import io.infinitic.workflowManager.common.parser.setPropertiesToObject
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskInput
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.workflowManager.worker.data.MethodContext
import io.infinitic.workflowManager.worker.exceptions.KnownStepException
import io.infinitic.workflowManager.worker.exceptions.NewStepException
import java.lang.reflect.InvocationTargetException

class WorkflowTaskImpl : WorkflowTask {
    override fun handle(input: WorkflowTaskInput): WorkflowTaskOutput {
        // get  instance workflow by name
        val workflowInstance = getNewInstancePerName("${input.workflowName}") as Workflow

        // invoke methods
        for (branch in input.branches) {
            // set branch initial properties
            val properties = branch.propertiesAtStart.mapValues { input.store[it.value] }
            setPropertiesToObject(workflowInstance, properties)

            // get method
            val method = if (branch.workflowMethod.workflowMethodParameterTypes == null) {
                getMethodPerNameAndParameterCount(
                    workflowInstance,
                    branch.workflowMethod.workflowMethodName,
                    branch.workflowMethodInput.size
                )
            } else {
                getMethodPerNameAndParameterTypes(
                    workflowInstance,
                    branch.workflowMethod.workflowMethodName,
                    branch.workflowMethod.workflowMethodParameterTypes!!
                )
            }

            // set branchContext
            val methodContext = MethodContext(
                workflowName = input.workflowName,
                workflowId = input.workflowId,
                workflowOptions = input.workflowOptions,
                pastInstructions = branch.pastInstructions
            )
            workflowInstance.setMethodContext(methodContext)
            // run it
            val output = try {
                method.invoke(workflowInstance, *branch.workflowMethodInput.data)
            } catch (e: InvocationTargetException) {
                when (e.cause) {
                    is NewStepException -> {
                        methodContext.newStep = Step.Id((e.cause as NewStepException).newCommand.commandId)
                    }
                    is KnownStepException -> Unit
                    else -> throw e.cause!!
                }
            } finally {
                // send WorkflowTaskOutput(workflowId, newStep, newCommands)
            }
        }
        // run method
        return WorkflowTaskOutput()
    }
}
