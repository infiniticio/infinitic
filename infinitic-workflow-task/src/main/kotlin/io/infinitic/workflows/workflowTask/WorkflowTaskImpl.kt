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
import io.infinitic.common.workers.config.WorkflowVersion
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
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.setChannelNames
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class WorkflowTaskImpl : WorkflowTask {

    lateinit var checkMode: WorkflowCheckMode
    lateinit var instance: Workflow
    lateinit var method: Method

    override fun handle(workflowTaskParameters: WorkflowTaskParameters): WorkflowTaskReturnValue {
        // get method
        val methodRun = workflowTaskParameters.methodRun

        // set context
        with(workflowTaskParameters) {
            instance.workflowName = workflowName.toString()
            instance.workflowId = workflowId.toString()
            instance.methodName = methodRun.methodName.toString()
            instance.methodId = methodRun.methodRunId.toString()
            instance.tags = workflowTags.map { it.tag }.toSet()
            instance.meta = workflowMeta.map
        }
        val dispatcher = WorkflowDispatcherImpl(checkMode, workflowTaskParameters)
        instance.dispatcher = dispatcher

        // define setProperties function
        val setProperties = { nameHashes: Map<PropertyName, PropertyHash> ->
            // in case properties contain some Deferred
            Deferred.setWorkflowDispatcher(dispatcher)
            instance.setProperties(workflowTaskParameters.workflowPropertiesHashValue, nameHashes)
            Deferred.delWorkflowDispatcher()
        }

        // give it to dispatcher
        dispatcher.setProperties = setProperties

        // set workflow's initial properties
        setProperties(methodRun.propertiesNameHashAtStart)

        // initialize name of channels for this workflow, based on the methods that provide them
        instance.setChannelNames()

        // get method parameters
        // in case parameters contain some Deferred
        Deferred.setWorkflowDispatcher(dispatcher)
        val parameters = methodRun.methodParameters.map { it.deserialize() }.toTypedArray()
        Deferred.delWorkflowDispatcher()

        // run method and get return value (null if end not reached)
        val methodReturnValue = try {
            ReturnValue.from(method.invoke(instance, *parameters))
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

        return WorkflowTaskReturnValue(
            newCommands = dispatcher.newCommands,
            newStep = dispatcher.newStep,
            properties = instance.getProperties(),
            methodReturnValue = methodReturnValue,
            workflowVersion = WorkflowVersion.from(instance::class.java)
        )
    }
}
