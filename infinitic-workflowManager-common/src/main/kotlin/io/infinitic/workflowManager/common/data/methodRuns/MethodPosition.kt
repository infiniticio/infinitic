package io.infinitic.workflowManager.common.data.methodRuns

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class MethodPosition @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@get:JsonValue val position: String) {
    override fun toString() = position
}
