// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.worker.workflowTask

import io.infinitic.common.tasks.data.MethodOutput
import io.infinitic.common.tasks.parser.getMethodPerNameAndParameterCount
import io.infinitic.common.tasks.parser.getMethodPerNameAndParameterTypes
import io.infinitic.common.workflows.Workflow
import io.infinitic.common.workflows.WorkflowTaskContext
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.properties.PropertiesHashValue
import io.infinitic.common.workflows.data.properties.PropertiesNameHash
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskInput
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.common.workflows.parser.getPropertiesFromObject
import io.infinitic.common.workflows.parser.setPropertiesToObject
import io.infinitic.worker.task.TaskAttemptContext
import java.lang.RuntimeException
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.jvm.javaType

class WorkflowTaskImpl : WorkflowTask {
    private lateinit var taskAttemptContext: TaskAttemptContext

    override fun handle(workflowTaskInput: WorkflowTaskInput): WorkflowTaskOutput {
        // get  instance workflow by name
        val workflowInstance = taskAttemptContext.worker.getWorkflow("${workflowTaskInput.workflowName}")

        // set methodContext
        val workflowTaskContext = WorkflowTaskContextImpl(workflowTaskInput, workflowInstance)

        // run method and get output
        val methodOutput = workflowTaskContext.run()

        return WorkflowTaskOutput(
            workflowTaskInput.workflowId,
            workflowTaskInput.methodRun.methodRunId,
            workflowTaskContext.newCommands,
            workflowTaskContext.newSteps,
            PropertiesNameHash(),
            PropertiesHashValue(),
            methodOutput
        )
    }


}
