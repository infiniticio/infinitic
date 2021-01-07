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

import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.executors.parser.getPropertiesFromObject
import io.infinitic.common.workflows.executors.parser.setPropertiesToObject
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowTaskContext
import java.lang.RuntimeException
import kotlin.reflect.jvm.javaType

fun setWorkflowProperties(
    workflow: Workflow,
    propertiesHashValue: Map<PropertyHash, PropertyValue>,
    propertiesNameHash: Map<PropertyName, PropertyHash>
) {
    val properties = propertiesNameHash.mapValues {
        propertiesHashValue[it.value] ?: throw RuntimeException("This should not happen: unknown hash ${it.value} in $propertiesHashValue")
    }

    setPropertiesToObject(workflow, properties)
}

/*
get current workflow properties (WorkflowTaskContext and proxies excluded)
TODO: manage Deferred in properties (including WorkflowTaskContext property)
 */
fun getWorkflowProperties(workflow: Workflow) = getPropertiesFromObject(
    workflow,
    {
        it.third.javaType.typeName != WorkflowTaskContext::class.java.name &&
            ! it.second!!::class.java.name.startsWith("com.sun.proxy.")
    }
)
