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

import io.infinitic.annotations.Ignore
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.executors.parser.getPropertiesFromObject
import io.infinitic.common.workflows.executors.parser.setPropertiesToObject
import io.infinitic.workflows.Channel
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowContext
import java.lang.RuntimeException
import java.lang.reflect.Proxy
import kotlin.reflect.full.createType
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.javaType

internal fun Workflow.setProperties(
    propertiesHashValue: Map<PropertyHash, PropertyValue>,
    propertiesNameHash: Map<PropertyName, PropertyHash>
) {
    val properties = propertiesNameHash.mapValues {
        propertiesHashValue[it.value] ?: throw RuntimeException("This should not happen: unknown hash ${it.value} in $propertiesHashValue")
    }

    setPropertiesToObject(this, properties)
}

// TODO: manage Deferred in properties
internal fun Workflow.getProperties() = getPropertiesFromObject(this) {
    // excludes context
    it.first.returnType.javaType.typeName != WorkflowContext::class.java.name &&
        // excludes Channels
        !it.first.returnType.isSubtypeOf(Channel::class.starProjectedType) &&
        // excludes Proxies (tasks and workflows)
        !Proxy.isProxyClass(it.second!!::class.java) &&
        // exclude SLF4J loggers
        !it.first.returnType.isSubtypeOf(org.slf4j.Logger::class.createType()) &&
        // exclude Ignore annotation
        !it.first.hasAnnotation<Ignore>()
}
