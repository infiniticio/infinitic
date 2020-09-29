package io.infinitic.common.tasks.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.interfaces.IntInterface

data class TaskAttemptIndex
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override var int: kotlin.Int = 0) : IntInterface
