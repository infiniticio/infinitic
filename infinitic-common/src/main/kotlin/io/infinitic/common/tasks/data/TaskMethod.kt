package io.infinitic.common.tasks.data

import java.lang.reflect.Method

data class TaskMethod(
    val methodName: String,
    val methodParameterTypes: List<String>?
) {
    companion object {
        fun from(method: Method) = TaskMethod(
            methodName = method.name,
            methodParameterTypes = method.parameterTypes.map { it.name }
        )
    }
}
