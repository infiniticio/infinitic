package io.infinitic.taskManager.common.data

import com.fasterxml.jackson.core.JsonProcessingException
import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.exceptions.ErrorDuringJsonDeserializationOfParameter
import io.infinitic.taskManager.common.exceptions.ErrorDuringJsonSerializationOfParameter
import io.infinitic.taskManager.common.exceptions.InconsistentJsonSerializationOfParameter
import java.lang.reflect.Method

abstract class Input(open vararg val data: Any?) {
    lateinit var serializedData: List<SerializedData>

    companion object {
        fun deserialize(serialized: List<SerializedData>) =
            serialized.map { it.deserialize() }.toTypedArray()
    }

    fun getSerialized(method: Method? = null) = when {
        this::serializedData.isInitialized -> serializedData
        method == null -> data.map { SerializedData.from(it) }
        else -> data.mapIndexed { index, value ->
            getSerializedData(
                parameterName = method.parameters[index].name,
                parameterValue = value,
                parameterType = method.parameterTypes[index],
                methodName = method.name,
                className = method.declaringClass.name
            )
        }
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Input

        return data.contentDeepEquals((other as Input).data)
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}
