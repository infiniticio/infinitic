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

import io.infinitic.common.data.ClientName
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.parser.getMethodPerNameAndParameters
import io.infinitic.common.workers.config.WorkflowCheckMode
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskReturnValue
import io.infinitic.exceptions.DeferredException
import io.infinitic.exceptions.FailedWorkflowTaskException
import io.infinitic.exceptions.WorkerException
import io.infinitic.tasks.Task
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.setChannelNames
import java.lang.reflect.InvocationTargetException

class WorkflowTaskImpl : WorkflowTask {

    lateinit var checkMode: WorkflowCheckMode

    override fun handle(workflowTaskParameters: WorkflowTaskParameters): WorkflowTaskReturnValue {
        // get  instance workflow by name
        val workflow =
            Task.context.get().workerRegistry.getRegisteredWorkflow(workflowTaskParameters.workflowName).factory()

        // get method
        val methodRun = workflowTaskParameters.methodRun
        val method = getMethodPerNameAndParameters(
            workflow::class.java,
            "${methodRun.methodName}",
            methodRun.methodParameterTypes?.types,
            methodRun.methodParameters.size
        )

        // set context
        with(workflowTaskParameters) {
            workflow.workflowName = workflowName.toString()
            workflow.workflowId = workflowId.toString()
            workflow.methodName = methodRun.methodName.toString()
            workflow.methodId = methodRun.methodRunId.toString()
            workflow.tags = workflowTags.map { it.tag }.toSet()
            workflow.meta = workflowMeta.map
        }
        val dispatcher = WorkflowDispatcherImpl(checkMode, workflowTaskParameters)
        workflow.dispatcher = dispatcher

        // define setProperties function
        val setProperties = { nameHashes: Map<PropertyName, PropertyHash> ->
            // in case properties contain some Deferred
            Deferred.setWorkflowDispatcher(dispatcher)
            workflow.setProperties(workflowTaskParameters.workflowPropertiesHashValue, nameHashes)
            Deferred.delWorkflowDispatcher()
        }

        // give it to dispatcher
        dispatcher.setProperties = setProperties

        // set workflow's initial properties
        setProperties(methodRun.propertiesNameHashAtStart)

        // initialize name of channels for this workflow, based on the methods that provide them
        workflow.setChannelNames()

        // get method parameters
        // in case parameters contain some Deferred
        Deferred.setWorkflowDispatcher(dispatcher)
        val parameters = methodRun.methodParameters.map { it.deserialize() }.toTypedArray()
        Deferred.delWorkflowDispatcher()

        // run method and get return value (null if end not reached)
        val methodReturnValue = try {
            ReturnValue.from(method.invoke(workflow, *parameters))
        } catch (e: InvocationTargetException) {
            when (val cause = e.cause) {
                // we reach an uncompleted step
                is WorkflowTaskException -> null
                // the errors below will be caught by the task executor
                is DeferredException -> throw cause
                // Send back other exceptions
                is Exception -> throw FailedWorkflowTaskException(
                    workflowName = workflowTaskParameters.workflowName.toString(),
                    workflowId = workflowTaskParameters.workflowId.toString(),
                    workflowTaskId = Task.taskId,
                    workerException = WorkerException.from(ClientName(Task.workerName), cause)
                )
                // Throwable are not caught
                else -> throw cause!!
            }
        }

        val properties = workflow.getProperties()

        return WorkflowTaskReturnValue(
            newCommands = dispatcher.newCommands,
            newStep = dispatcher.newStep,
            properties = properties,
            methodReturnValue = methodReturnValue
        )
    }
}
