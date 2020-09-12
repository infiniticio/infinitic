package io.infinitic.workflowManager.common.data.instructions

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class StringPosition @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@get:JsonValue val position: String) {
    override fun toString() = position
}
