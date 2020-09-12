package io.infinitic.workflowManager.worker

import io.infinitic.taskManager.common.parser.getMethodPerNameAndParameterCount
import io.infinitic.taskManager.common.parser.getMethodPerNameAndParameterTypes
import io.infinitic.taskManager.common.parser.getNewInstancePerName
import io.infinitic.workflowManager.common.data.steps.Step
import io.infinitic.workflowManager.common.parser.setPropertiesToObject
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskInput
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.workflowManager.worker.data.MethodExecutionContext
import io.infinitic.workflowManager.worker.exceptions.KnownStepException
import io.infinitic.workflowManager.worker.exceptions.NewStepException
import java.lang.reflect.InvocationTargetException

class WorkflowTaskImpl : WorkflowTask {
    override fun handle(input: WorkflowTaskInput): WorkflowTaskOutput {
        // get  instance workflow by name
        val workflowInstance = getNewInstancePerName("${input.workflowName}") as Workflow

        // set initial properties
        val properties = input.method.methodPropertiesAtStart.mapValues { input.workflowPropertyStore[it.value] }
        setPropertiesToObject(workflowInstance, properties)

        // get method
        val method = if (input.method.methodName.methodParameterTypes == null) {
            getMethodPerNameAndParameterCount(
                workflowInstance,
                input.method.methodName.methodName,
                input.method.methodInput.size
            )
        } else {
            getMethodPerNameAndParameterTypes(
                workflowInstance,
                input.method.methodName.methodName,
                input.method.methodName.methodParameterTypes!!
            )
        }

        // set methodContext
        val methodContext = MethodExecutionContext(input, workflowInstance)
        workflowInstance.methodExecutionContext = methodContext

        // run it
        val output = try {
            method.invoke(workflowInstance, *input.method.methodInput.data)
        } catch (e: InvocationTargetException) {
            when (e.cause) {
                is NewStepException -> Unit
                is KnownStepException -> Unit
                else -> throw e.cause!!
            }
        }

        // send WorkflowTaskOutput(workflowId, newStep, newCommands)
        return WorkflowTaskOutput()
    }
}
