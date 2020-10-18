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
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaType

class WorkflowTaskImpl : WorkflowTask {
    private lateinit var taskAttemptContext: TaskAttemptContext

    override fun handle(workflowTaskInput: WorkflowTaskInput): WorkflowTaskOutput {
        // get  instance workflow by name
        val workflowInstance = taskAttemptContext.worker.getWorkflow("${workflowTaskInput.workflowName}")

        // set methodContext
        val workflowTaskContext = WorkflowTaskContextImpl(workflowTaskInput, workflowInstance)

        // set workflow task context
        workflowInstance.context = workflowTaskContext

        // set workflow's initial properties
        setPropertiesToObject(
            workflowInstance,
            workflowTaskInput.methodRun.getPropertiesNameValue(workflowTaskInput.workflowPropertiesHashValue)
        )

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

        // get current workflow properties (WorkflowTaskContext and proxies excluded)
        val currentPropertiesNameValue = getPropertiesFromObject(workflowInstance, {
            it.third.javaType.typeName != WorkflowTaskContext::class.java.name &&
            ! it.second!!::class.java.name.startsWith("com.sun.proxy.")
        })

        // get properties updates
        val unknownProperties = workflowTaskInput.methodRun.propertiesNameHashAtStart.keys.filter { it !in currentPropertiesNameValue.keys }.joinToString()
        if (unknownProperties.isNotEmpty()) throw RuntimeException(unknownProperties)

        val hashValueUpdates = mutableMapOf<PropertyHash, PropertyValue>()
        val nameHashUpdates = mutableMapOf<PropertyName, PropertyHash>()

        currentPropertiesNameValue.map {
            val hash = it.value.hash()
            if (it.key !in workflowTaskInput.methodRun.propertiesNameHashAtStart.keys || hash != workflowTaskInput.methodRun.propertiesNameHashAtStart[it.key]) {
                // new property
                nameHashUpdates[it.key] = hash
            }
            if (hash !in hashValueUpdates.keys) {
                hashValueUpdates[hash] = it.value
            }
        }

        return WorkflowTaskOutput(
            workflowTaskInput.workflowId,
            workflowTaskInput.methodRun.methodRunId,
            workflowTaskContext.newCommands,
            workflowTaskContext.newSteps,
            PropertiesNameHash(nameHashUpdates),
            PropertiesHashValue(hashValueUpdates),
            methodOutput
        )
    }

    private fun getMethod(workflow: Workflow, methodRun: MethodRun) = if (methodRun.methodParameterTypes.types == null) {
        getMethodPerNameAndParameterCount(
            workflow,
            "${methodRun.methodName}",
            methodRun.methodInput.size
        )
    } else {
        getMethodPerNameAndParameterTypes(
            workflow,
            "${methodRun.methodName}",
            methodRun.methodParameterTypes.types!!
        )
    }
}
