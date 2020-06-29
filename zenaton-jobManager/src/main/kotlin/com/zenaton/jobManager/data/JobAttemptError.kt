package com.zenaton.jobManager.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.common.data.SerializedData
import com.zenaton.common.data.interfaces.DataInterface
import com.zenaton.commons.data.interfaces.ErrorInterface
import com.zenaton.commons.data.interfaces.OutputInterface

data class JobAttemptError
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val error: SerializedData) : ErrorInterface

