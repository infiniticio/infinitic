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
