package com.zenaton.jobManager.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.commons.data.interfaces.IntInterface

data class JobAttemptRetry
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    constructor(@get:JsonValue override var int: Int = 0) : IntInterface {
    operator fun inc(): JobAttemptRetry { int++; return this }
    operator fun plus(increment: Int): JobAttemptRetry = JobAttemptRetry(int + increment)
}
