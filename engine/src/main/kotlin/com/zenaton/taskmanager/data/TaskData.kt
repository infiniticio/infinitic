package com.zenaton.taskmanager.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.commons.data.interfaces.DataInterface

data class TaskData
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    constructor(@get:JsonValue override val data: ByteArray) : DataInterface {
    final override fun equals(other: Any?) = equalsData(other)
    final override fun hashCode() = hashCodeData()
}
