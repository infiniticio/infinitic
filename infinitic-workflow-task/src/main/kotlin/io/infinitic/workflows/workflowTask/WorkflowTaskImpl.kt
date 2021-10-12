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

package io.infinitic.workflows.workflowTask

import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.parser.getMethodPerNameAndParameters
import io.infinitic.common.workflows.data.channels.ChannelImpl
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskReturnValue
import io.infinitic.exceptions.workflows.MultipleNamesForChannelException
import io.infinitic.exceptions.workflows.NonUniqueChannelFromChannelMethodException
import io.infinitic.exceptions.workflows.ParametersInChannelMethodException
import io.infinitic.tasks.Task
import io.infinitic.workflows.Channel
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.Workflow
import java.lang.reflect.InvocationTargetException
import java.time.Duration

class WorkflowTaskImpl : Task(), WorkflowTask {

    override fun getDurationBeforeRetry(e: Exception): Duration? = null

    override fun handle(workflowTaskParameters: WorkflowTaskParameters): WorkflowTaskReturnValue {
        // get  instance workflow by name
        val workflow = context.register.getWorkflowInstance("${workflowTaskParameters.workflowName}")

        // setProperties function
        val setProperties = {
            hashValues: Map<PropertyHash, PropertyValue>,
            nameHashes: Map<PropertyName, PropertyHash>
            ->
            workflow.setProperties(hashValues, nameHashes)
        }

        // set workflow's initial properties
        setProperties(
            workflowTaskParameters.workflowPropertiesHashValue,
            workflowTaskParameters.methodRun.propertiesNameHashAtStart
        )

        // set context
        workflow.context = WorkflowContextImpl(workflowTaskParameters)
        workflow.dispatcher = WorkflowDispatcherImpl(workflowTaskParameters, setProperties)

        // initialize name of channels for this workflow, based on the methods that provide them
        setChannelNames(workflow)

        // get method
        val methodRun = workflowTaskParameters.methodRun
        val method = getMethodPerNameAndParameters(
            workflow::class.java,
            "${methodRun.methodName}",
            methodRun.methodParameterTypes?.types,
            methodRun.methodParameters.size
        )

        // get method parameters
        // just in case those parameters contain some Deferred, we set the current dispatcher in the LocalThread
        Deferred.setWorkflowDispatcher(workflow.dispatcher)
        val parameters = methodRun.methodParameters.map { it.deserialize() }.toTypedArray()
        // to avoid any side effect, we clear the LocalThread right away
        Deferred.delWorkflowDispatcher()

        // run method and get return value (null if end not reached)
        val methodReturnValue = try {
            MethodReturnValue.from(method.invoke(workflow, *parameters))
        } catch (e: InvocationTargetException) {
            when (e.cause) {
                is WorkflowTaskException -> null
                else -> throw e.cause ?: e // this error will be caught by the task executor
            }
        }

        val properties = workflow.getProperties()

        return WorkflowTaskReturnValue(
            (workflow.dispatcher as WorkflowDispatcherImpl).newCommands,
            (workflow.dispatcher as WorkflowDispatcherImpl).newStep,
            properties,
            methodReturnValue
        )
    }

    private fun setChannelNames(workflow: Workflow) {
        workflow::class.java.declaredMethods
            .filter { it.returnType.name == Channel::class.java.name }
            .map {
                // channel must not have parameters
                if (it.parameterCount > 0) {
                    throw ParametersInChannelMethodException(workflow::class.java.name, it.name)
                }
                // channel must be created only once per method
                it.isAccessible = true
                val channel = it.invoke(workflow)
                val channelBis = it.invoke(workflow)
                if (channel !== channelBis) {
                    throw NonUniqueChannelFromChannelMethodException(workflow::class.java.name, it.name)
                }
                // this channel must not have a name already
                channel as ChannelImpl<*>
                if (channel.isNameInitialized()) {
                    throw MultipleNamesForChannelException(workflow::class.java.name, it.name, channel.name)
                }
                // set channel name
                channel.name = it.name
            }
    }
}
