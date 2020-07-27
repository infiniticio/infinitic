package com.zenaton.jobManager.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.common.data.SerializedData
import com.zenaton.commons.data.interfaces.ErrorInterface

data class JobAttemptError
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val error: SerializedData) : ErrorInterface {
    constructor(error: Any) : this(SerializedData.from(error))
}
