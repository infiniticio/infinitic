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

package io.infinitic.common.serDe.kserializer

import io.infinitic.common.clients.messages.ClientResponseEnvelope
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import kotlinx.serialization.KSerializer
import java.lang.reflect.Modifier.isStatic
import kotlin.reflect.KClass

// @OptIn(ExperimentalStdlibApi::class)
// fun getKSerializerOrNull(klass: Class<*>) = try {
//    @Suppress("UNCHECKED_CAST")
//    serializer(klass.kotlin.createType())
// } catch (e: Exception) {
//    null
// }

fun getKSerializerOrNull(klass: Class<*>): KSerializer<*>? {
    val companionField = klass.declaredFields.find {
        it.name == "Companion" && isStatic(it.modifiers)
    } ?: return null
    val companion = companionField.get(klass)
    val serializerMethod = try {
        companion::class.java.getMethod("serializer")
    } catch (e: NoSuchMethodException) {
        return null
    }
    if (serializerMethod.returnType.name != KSerializer::class.qualifiedName) {
        return null
    }
    @Suppress("UNCHECKED_CAST")
    return serializerMethod.invoke(companion) as KSerializer<*>
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> kserializer(klass: KClass<T>) = when (klass) {
    ClientResponseEnvelope::class -> ClientResponseEnvelope.serializer()
    WorkflowEngineEnvelope::class -> WorkflowEngineEnvelope.serializer()
    TaskEngineEnvelope::class -> TaskEngineEnvelope.serializer()
    TaskExecutorMessage::class -> TaskExecutorMessage.serializer()
    MonitoringGlobalEnvelope::class -> MonitoringGlobalEnvelope.serializer()
    MonitoringPerNameEnvelope::class -> MonitoringPerNameEnvelope.serializer()
    else -> throw RuntimeException("This should not happen: applying kserializer with ${klass.qualifiedName}")
} as KSerializer <T>
