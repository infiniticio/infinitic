package com.zenaton.jobManager.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.common.data.SerializedData
import com.zenaton.commons.data.interfaces.OutputInterface

data class JobOutput
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val output: SerializedData) : OutputInterface
