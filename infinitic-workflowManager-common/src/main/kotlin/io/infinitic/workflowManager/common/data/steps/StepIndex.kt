package io.infinitic.workflowManager.common.data.steps

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.interfaces.IntInterface

data class StepIndex
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override var int: kotlin.Int = 0) : IntInterface
