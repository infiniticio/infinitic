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

package io.infinitic.common.data

import com.fasterxml.jackson.core.JsonProcessingException
import io.infinitic.common.serDe.SerializedData
import io.infinitic.exceptions.serialization.ErrorDuringJsonSerializationOfParameter
import java.lang.reflect.Method

abstract class Parameters(open vararg val serializedDataArray: SerializedData) {
    companion object {
        fun getSerializedParameter(method: Method, index: Int, parameterValue: Any?) = try {
            SerializedData.from(parameterValue)
        } catch (e: JsonProcessingException) {
            val parameterName = method.parameters[index].name
            val parameterType = method.parameterTypes[index]
            val methodName = method.name
            val className = method.declaringClass.name
            throw ErrorDuringJsonSerializationOfParameter(parameterName, parameterType.name, methodName, className)
        }
    }

    final override fun toString() = "${serializedDataArray.toList()}"

    final override fun hashCode() = serializedDataArray.contentHashCode()

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Parameters

        if (!serializedDataArray.contentDeepEquals(other.serializedDataArray)) return false

        return true
    }

    fun get() = serializedDataArray.map { it.deserialize() }
}
