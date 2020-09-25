package io.infinitic.common.workflowManager.data.workflows

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.taskManager.data.bases.Name
import java.lang.reflect.Method

data class WorkflowName
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val name: String) : Name(name) {
    companion object {
        fun from(method: Method) = WorkflowName(method.declaringClass.name)
    }
}
