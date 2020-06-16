package com.zenaton.jobManager.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class JobInput
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue val input: List<JobParameter>)
