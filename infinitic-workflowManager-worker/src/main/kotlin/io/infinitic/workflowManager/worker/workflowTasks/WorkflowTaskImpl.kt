package io.infinitic.workflowManager.worker.workflowTasks

import io.infinitic.taskManager.common.parser.getMethodPerNameAndParameterCount
import io.infinitic.taskManager.common.parser.getMethodPerNameAndParameterTypes
import io.infinitic.taskManager.worker.TaskAttemptContext
import io.infinitic.workflowManager.common.data.methodRuns.MethodOutput
import io.infinitic.workflowManager.common.data.methodRuns.MethodRun
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTask
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskInput
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.workflowManager.common.parser.setPropertiesToObject
import io.infinitic.workflowManager.worker.Workflow
import io.infinitic.workflowManager.worker.data.MethodRunContext
import java.lang.reflect.InvocationTargetException

class WorkflowTaskImpl : WorkflowTask {
    lateinit var taskAttemptContext: TaskAttemptContext

    override fun handle(input: WorkflowTaskInput): WorkflowTaskOutput {
        // get  instance workflow by name
        val workflowInstance = taskAttemptContext.worker.getTaskInstance("${input.workflowName}") as Workflow

        // set initial properties
        val properties = input.methodRun.propertiesAtStart.mapValues { input.workflowPropertyStore[it.value] }
        setPropertiesToObject(workflowInstance, properties)

        // get method
        val method = getMethod(workflowInstance, input.methodRun)

        // set methodContext
        val methodRunContext = MethodRunContext(input, workflowInstance)

        // run method and get output
        val methodOutput = try {
            workflowInstance.methodRunContext = methodRunContext

            MethodOutput(method.invoke(workflowInstance, *input.methodRun.methodInput.data))
        } catch (e: InvocationTargetException) {
            when (e.cause) {
                is NewStepException -> null
                is KnownStepException -> null
                else -> throw e.cause!!
            }
        }

        // TODO("Properties updates")
        return WorkflowTaskOutput(
            input.workflowId,
            input.methodRun.methodRunId,
            methodRunContext.newCommands,
            methodRunContext.newSteps,
            input.methodRun.propertiesAtStart,
            input.workflowPropertyStore,
            methodOutput
        )
    }

    private fun getMethod(workflow: Workflow, methodRun: MethodRun) = if (methodRun.methodName.methodParameterTypes == null) {
        getMethodPerNameAndParameterCount(
            workflow,
            methodRun.methodName.methodName,
            methodRun.methodInput.size
        )
    } else {
        getMethodPerNameAndParameterTypes(
            workflow,
            methodRun.methodName.methodName,
            methodRun.methodName.methodParameterTypes!!
        )
    }
}
