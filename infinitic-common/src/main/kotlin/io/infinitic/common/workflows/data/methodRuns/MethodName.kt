package io.infinitic.common.workflows.data.methodRuns

import java.lang.reflect.Method

data class MethodName(
    val methodName: String,
    val methodParameterTypes: List<String>?
) {
    companion object {
        fun from(method: Method) = MethodName(
            methodName = method.name,
            methodParameterTypes = method.parameterTypes.map { it.name }
        )
    }
}
