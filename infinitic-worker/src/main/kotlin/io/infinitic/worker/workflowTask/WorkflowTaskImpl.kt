package io.infinitic.worker.workflowTask

import io.infinitic.common.tasks.parser.getMethodPerNameAndParameterCount
import io.infinitic.common.tasks.parser.getMethodPerNameAndParameterTypes
import io.infinitic.common.workflows.Workflow
import io.infinitic.common.workflows.data.methodRuns.MethodOutput
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskInput
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.common.workflows.parser.setPropertiesToObject
import io.infinitic.worker.task.TaskAttemptContext
import java.lang.reflect.InvocationTargetException

class WorkflowTaskImpl : WorkflowTask {
    private lateinit var taskAttemptContext: TaskAttemptContext

    override fun handle(workflowTaskInput: WorkflowTaskInput): WorkflowTaskOutput {
        // get  instance workflow by name
        val workflowInstance = taskAttemptContext.worker.getWorkflowInstance("${workflowTaskInput.workflowName}")

        // set methodContext
        val workflowTaskContext = WorkflowTaskContextImpl(workflowTaskInput, workflowInstance)

        // set workflow task context
        workflowInstance.context = workflowTaskContext

        // set workflow's initial properties
        val properties = workflowTaskInput.methodRun.propertiesAtStart.mapValues {
            workflowTaskInput.workflowPropertyStore[it.value]
        }
        setPropertiesToObject(workflowInstance, properties)

        // get method
        val method = getMethod(workflowInstance, workflowTaskInput.methodRun)

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
            workflowTaskContext.newCommands,
            workflowTaskContext.newSteps,
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
