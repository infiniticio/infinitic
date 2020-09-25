package io.infinitic.worker.workflowManager.workflowTasks

import io.infinitic.common.taskManager.parser.getMethodPerNameAndParameterCount
import io.infinitic.common.taskManager.parser.getMethodPerNameAndParameterTypes
import io.infinitic.worker.taskManager.TaskAttemptContext
import io.infinitic.common.workflowManager.data.methodRuns.MethodOutput
import io.infinitic.common.workflowManager.data.methodRuns.MethodRun
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTaskInput
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.common.workflowManager.parser.setPropertiesToObject
import io.infinitic.worker.workflowManager.Workflow
import io.infinitic.worker.workflowManager.data.MethodRunContext
import java.lang.reflect.InvocationTargetException

class WorkflowTaskImpl : WorkflowTask {
    private lateinit var taskAttemptContext: TaskAttemptContext

    override fun handle(workflowTaskInput: WorkflowTaskInput): WorkflowTaskOutput {
        // get  instance workflow by name
        val workflowInstance = taskAttemptContext.worker.getInstance("${workflowTaskInput.workflowName}") as Workflow

        // set initial properties
        val properties = workflowTaskInput.methodRun.propertiesAtStart.mapValues {
            workflowTaskInput.workflowPropertyStore[it.value]
        }
        setPropertiesToObject(workflowInstance, properties)

        // get method
        val method = getMethod(workflowInstance, workflowTaskInput.methodRun)

        // set methodContext
        val methodRunContext = MethodRunContext(taskAttemptContext.worker, workflowTaskInput, workflowInstance)

        workflowInstance.methodRunContext = methodRunContext

        // run method and get output
        val methodOutput = try {
            MethodOutput(method.invoke(workflowInstance, *workflowTaskInput.methodRun.methodInput.data))
        } catch (e: InvocationTargetException) {
            when (e.cause) {
                is NewStepException -> null
                is KnownStepException -> null
                else -> throw e.cause!!
            }
        }

        // TODO("Properties updates")
        return WorkflowTaskOutput(
            workflowTaskInput.workflowId,
            workflowTaskInput.methodRun.methodRunId,
            methodRunContext.newCommands,
            methodRunContext.newSteps,
            workflowTaskInput.methodRun.propertiesAtStart,
            workflowTaskInput.workflowPropertyStore,
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
