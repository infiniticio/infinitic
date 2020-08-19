package com.zenaton.taskManager.common.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.common.data.SerializedData
import com.zenaton.taskManager.common.data.interfaces.InputInterface

data class JobInput
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val input: List<SerializedData>) : InputInterface {
    companion object {
        fun builder() = JobInputBuilder()
    }
}
