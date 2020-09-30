package io.infinitic.common.tasks.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.lang.reflect.Method

data class MethodParameterTypes
@JsonCreator constructor(@get:JsonValue val types: List<String>?) {
    companion object {
        fun from(method: Method) = MethodParameterTypes(method.parameterTypes.map { it.name })
    }
}
