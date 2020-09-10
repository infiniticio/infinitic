package io.infinitic.workflowManager.common.data.methods

import java.lang.reflect.Method

data class Method(
    val workflowMethodName: String,
    val workflowMethodParameterTypes: List<String>?
) {
    companion object {
        fun from(method: Method) = Method(
            workflowMethodName = method.name,
            workflowMethodParameterTypes = method.parameterTypes.map { it.name }
        )
    }
}
