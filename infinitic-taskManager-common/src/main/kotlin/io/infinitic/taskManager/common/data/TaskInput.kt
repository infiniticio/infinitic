package io.infinitic.taskManager.common.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.data.interfaces.InputInterface

data class TaskInput
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val input: List<SerializedData>) : InputInterface {
    companion object {
        fun builder() = TaskInputBuilder()
    }
}
