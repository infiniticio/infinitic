/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.tasks.executor.workflowTask

import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.parser.getMethodPerNameAndParameterCount
import io.infinitic.common.parser.getMethodPerNameAndParameterTypes
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.tasks.executor.task.TaskAttemptContext
import io.infinitic.workflows.Workflow
import java.lang.reflect.InvocationTargetException

class WorkflowTaskImpl : WorkflowTask {
    private lateinit var taskAttemptContext: TaskAttemptContext

    override fun handle(workflowTaskParameters: WorkflowTaskParameters): WorkflowTaskOutput {
        // get  instance workflow by name
        val workflow = taskAttemptContext.taskExecutor.getWorkflowInstance("${workflowTaskParameters.workflowName}")

        // set methodContext
        val workflowTaskContext = WorkflowTaskContextImpl(workflowTaskParameters, workflow)

        // set workflow's initial properties
        setWorkflowProperties(
            workflow,
            workflowTaskParameters.workflowPropertiesHashValue,
            workflowTaskParameters.methodRun.propertiesNameHashAtStart
        )

        // get method
        val method = getMethod(workflow, workflowTaskParameters.methodRun)

        // run method and get output (null if end not reached)
        val methodReturnValue = try {
            MethodReturnValue.from(method.invoke(workflow, *workflowTaskParameters.methodRun.methodParameters.get().toTypedArray()))
        } catch (e: InvocationTargetException) {
            when (e.cause) {
                is WorkflowTaskException -> null
                else -> throw e.cause!!
            }
        }

        val properties = getWorkflowProperties(workflow)

        return WorkflowTaskOutput(
            workflowTaskParameters.workflowId,
            workflowTaskParameters.methodRun.methodRunId,
            workflowTaskContext.newCommands,
            workflowTaskContext.newSteps,
            properties,
            methodReturnValue
        )
    }

    private fun getMethod(workflow: Workflow, methodRun: MethodRun) = if (methodRun.methodParameterTypes == null) {
        getMethodPerNameAndParameterCount(
            workflow,
            "${methodRun.methodName}",
            methodRun.methodParameters.size
        )
    } else {
        getMethodPerNameAndParameterTypes(
            workflow,
            "${methodRun.methodName}",
            methodRun.methodParameterTypes!!.types
        )
    }
}
