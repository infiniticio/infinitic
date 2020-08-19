package com.zenaton.taskManager.common.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.common.data.SerializedData
import com.zenaton.taskManager.common.data.interfaces.ErrorInterface

data class TaskAttemptError
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val error: SerializedData) : ErrorInterface {
    constructor(error: Throwable?) : this(SerializedData.from(error))
}
