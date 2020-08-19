package io.infinitic.taskManager.common.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.data.interfaces.ErrorInterface

data class TaskAttemptError
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val error: SerializedData) : ErrorInterface {
    constructor(error: Throwable?) : this(SerializedData.from(error))
}
