package com.zenaton.engine.tasks.data

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.zenaton.engine.interfaces.data.IdInterface
import java.util.UUID

data class TaskId(@JsonProperty(required = true) override val id: String = UUID.randomUUID().toString()) : IdInterface
