/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.workflows.workflowTask

import io.infinitic.common.data.methods.deserializeArgs
import io.infinitic.common.data.methods.encodeReturnValue
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskReturnValue
import io.infinitic.common.workflows.executors.getProperties
import io.infinitic.common.workflows.executors.setProperties
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.setChannelNames
import io.infinitic.workflows.setChannelTypes
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class WorkflowTaskImpl : WorkflowTask {

  lateinit var checkMode: WorkflowCheckMode
  lateinit var instance: Workflow
  lateinit var method: Method

  override fun handle(workflowTaskParameters: WorkflowTaskParameters): WorkflowTaskReturnValue {
    // get method
    val methodRun = workflowTaskParameters.workflowMethod

    // set dispatcher
    val dispatcher = WorkflowDispatcherImpl(checkMode, workflowTaskParameters)
    instance.dispatcher = dispatcher

    // define setProperties function
    val setProperties = { nameHashes: Map<PropertyName, PropertyHash> ->
      instance.setProperties(workflowTaskParameters.workflowPropertiesHashValue, nameHashes)
    }

    // give it to dispatcher
    dispatcher.setProperties = setProperties

    // set workflow's initial properties
    setProperties(methodRun.propertiesNameHashAtStart)

    // initialize name of channels for this workflow
    instance.setChannelNames()

    // initialize type of channels for this workflow
    instance.setChannelTypes()

    // get method parameters
    val parameters = method.deserializeArgs(methodRun.methodParameters)

    // run method and get return value (null if end not reached)
    val methodReturnValue = try {
      // method is the workflow method currently processed
      method.encodeReturnValue(method.invoke(instance, *parameters))
    } catch (e: InvocationTargetException) {
      when (val cause = e.cause ?: e) {
        // we reach an uncompleted step
        is WorkflowStepException -> null
        else -> throw cause
      }
    }

    return WorkflowTaskReturnValue(
        newCommands = dispatcher.newPastCommands,
        newStep = dispatcher.newCurrentStep,
        properties = instance.getProperties(),
        methodReturnValue = methodReturnValue,
        workflowVersion = WorkflowVersion.from(
            instance::
            class.java,
        ),
        workflowTaskInstant = workflowTaskParameters.workflowTaskInstant,
    )
  }
}
