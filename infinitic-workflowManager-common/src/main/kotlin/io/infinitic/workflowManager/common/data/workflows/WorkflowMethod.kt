package io.infinitic.workflowManager.common.data.workflows

import java.lang.reflect.Method

data class WorkflowMethod(
    val workflowMethodName: String,
    val workflowMethodParameterTypes: List<String>?
) {
    companion object {
        fun from(method: Method) = WorkflowMethod(
            workflowMethodName = method.name,
            workflowMethodParameterTypes = method.parameterTypes.map { it.name }
        )
    }
}
