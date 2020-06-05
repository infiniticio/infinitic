package com.zenaton.jobManager.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.commons.data.interfaces.IntInterface

data class JobAttemptIndex
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    constructor(@get:JsonValue override var int: Int = 0) : IntInterface {
    operator fun inc(): JobAttemptIndex { int++; return this }
    operator fun plus(increment: Int): JobAttemptIndex = JobAttemptIndex(int + increment)
}
