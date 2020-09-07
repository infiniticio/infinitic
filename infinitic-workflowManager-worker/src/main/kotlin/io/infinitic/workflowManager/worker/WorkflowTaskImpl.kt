package io.infinitic.workflowManager.worker

import io.infinitic.taskManager.common.parser.getMethodPerNameAndParameterCount
import io.infinitic.taskManager.common.parser.getMethodPerNameAndParameterTypes
import io.infinitic.taskManager.common.parser.getNewInstancePerName
import io.infinitic.workflowManager.common.parser.setPropertiesToObject
import io.infinitic.workflowManager.common.data.commands.PastCommand
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskInput
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.workflowManager.worker.data.BranchContext
import io.infinitic.workflowManager.worker.exceptions.NewStepException
import java.lang.reflect.InvocationTargetException

class WorkflowTaskImpl : WorkflowTask {
    override fun handle(input: WorkflowTaskInput): WorkflowTaskOutput {
        // get  instance workflow by name
        val workflowInstance = getNewInstancePerName(input.workflowName.name) as Workflow

        // invoke methods
        val commands = mutableListOf<PastCommand>()

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
            workflowInstance.branchContext = BranchContext(
                workflowName = input.workflowName,
                workflowId = input.workflowId,
                pastSteps = branch.pastSteps,
                pastCommands = branch.pastCommands
            )
            // run it
            val output = try {
                method.invoke(workflowInstance, *branch.workflowMethodInput.data)
            } catch (e: InvocationTargetException) {
                when (e.cause) {
                    is NewStepException -> Unit
                    else -> throw e.cause!!
                }
            }
        }
        // run method
        return WorkflowTaskOutput()
    }
}
