package io.infinitic.worker.workflowTask

import io.infinitic.common.taskManager.parser.getMethodPerNameAndParameterCount
import io.infinitic.common.taskManager.parser.getMethodPerNameAndParameterTypes
import io.infinitic.common.workflowManager.Workflow
import io.infinitic.common.workflowManager.WorkflowTaskContext
import io.infinitic.common.workflowManager.data.methodRuns.MethodOutput
import io.infinitic.common.workflowManager.data.methodRuns.MethodRun
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTaskInput
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.common.workflowManager.exceptions.BadWorkflowConstructor
import io.infinitic.common.workflowManager.parser.setPropertiesToObject
import io.infinitic.worker.task.TaskAttemptContext
import java.lang.reflect.InvocationTargetException

class WorkflowTaskImpl : WorkflowTask {
    private lateinit var taskAttemptContext: TaskAttemptContext

    override fun handle(workflowTaskInput: WorkflowTaskInput): WorkflowTaskOutput {
        // set methodContext
        val workflowTaskContext = WorkflowTaskContext(workflowTaskInput)

        // get  instance workflow by name
        val workflowClass = taskAttemptContext.worker.getWorkflowClass("${workflowTaskInput.workflowName}")

        // get constructor
        val constructor = try {
            workflowClass.getDeclaredConstructor(WorkflowTaskContext::class.java)
        } catch (e: NoSuchMethodException) {
            throw BadWorkflowConstructor("${workflowTaskInput.workflowName}")
        }

        val workflowInstance = constructor.newInstance(workflowTaskContext)

        workflowTaskContext.workflowInstance = workflowInstance

        // set initial properties
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
