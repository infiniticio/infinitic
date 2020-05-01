package com.zenaton.engine.data.decisions

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.engine.data.DataInterface

data class DecisionAttemptError @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@get:JsonValue override val data: ByteArray) :
    DataInterface {
    final override fun equals(other: Any?) = equalsData(other)
    final override fun hashCode() = hashCodeData()
}
