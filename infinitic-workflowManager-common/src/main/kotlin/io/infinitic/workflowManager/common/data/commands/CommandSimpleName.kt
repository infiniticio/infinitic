package io.infinitic.workflowManager.common.data.commands

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.taskManager.common.Constants
import io.infinitic.taskManager.common.data.bases.Name
import java.lang.reflect.Method

data class CommandSimpleName
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val name: String) : Name(name) {
    companion object {
        fun fromMethod(method: Method) = CommandSimpleName("${method.declaringClass.simpleName}${Constants.METHOD_DIVIDER}${method.name}")
    }
}
