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

package io.infinitic.common.tasks.data.bases

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.core.JsonProcessingException
import io.infinitic.common.data.SerializedData
import io.infinitic.common.tasks.exceptions.ErrorDuringJsonDeserializationOfParameter
import io.infinitic.common.tasks.exceptions.ErrorDuringJsonSerializationOfParameter
import io.infinitic.common.tasks.exceptions.InconsistentJsonSerializationOfParameter
import java.lang.reflect.Method
import kotlin.reflect.full.primaryConstructor

abstract class Input(open vararg val data: Any?) {
    @get:JsonValue
    val json get() = when {
        this::serializedData.isInitialized -> serializedData
        else -> data.map { SerializedData.from(it) }
    }

    lateinit var serializedData: List<SerializedData>

    companion object {
        inline fun <reified T : Input> fromSerialized(serialized: List<SerializedData>) =
            T::class.primaryConstructor!!.call(deserialize(serialized)).apply {
                this.serializedData = serialized
            }

        inline fun <reified T : Input> from(method: Method, data: Array<out Any>) =
            T::class.primaryConstructor!!.call(data).apply {
                serializedData = when {
                    this::serializedData.isInitialized -> serializedData
                    else -> data.mapIndexed { index, value ->
                        `access$getSerializedData`(
                            parameterName = method.parameters[index].name,
                            parameterValue = value,
                            parameterType = method.parameterTypes[index],
                            methodName = method.name,
                            className = method.declaringClass.name
                        )
                    }
                }
            }

        fun deserialize(serialized: List<SerializedData>) =
            serialized.map { it.deserialize() }.toTypedArray()
    }

    private fun getSerializedData(
        parameterName: String,
        parameterValue: Any?,
        parameterType: Class<*>,
        methodName: String,
        className: String
    ): SerializedData {
        val data: SerializedData
        val restoredValue: Any?
        // get serialized data
        try {
            data = SerializedData.from(parameterValue)
        } catch (e: JsonProcessingException) {
            throw ErrorDuringJsonSerializationOfParameter(parameterName, parameterValue, parameterType.name, methodName, className)
        }
        // for user convenience, we check right here that data can actually be deserialized
        try {
            restoredValue = data.deserialize()
        } catch (e: JsonProcessingException) {
            throw ErrorDuringJsonDeserializationOfParameter(parameterName, parameterValue, parameterType.name, methodName, className)
        }
        // check that serialization/deserialization process works as expected
        if (parameterValue != restoredValue) throw InconsistentJsonSerializationOfParameter(parameterName, parameterValue, restoredValue, parameterType.name, methodName, className)

        return data
    }

    final override fun hashCode(): Int {
        return data.contentHashCode()
    }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Input

        if (!data.contentDeepEquals(other.data)) return false

        return true
    }

    @PublishedApi
    internal fun `access$getSerializedData`(parameterName: String, parameterValue: Any?, parameterType: Class<*>, methodName: String, className: String) = getSerializedData(parameterName, parameterValue, parameterType, methodName, className)
}
