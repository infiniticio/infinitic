package io.infinitic.common.tasks.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.tasks.data.bases.Name
import java.lang.reflect.Method

data class TaskName
@JsonCreator constructor(@get:JsonValue override val name: String) : Name(name) {
    companion object {
        fun from(method: Method) = TaskName(Name.fromMethod(method))
    }
}
