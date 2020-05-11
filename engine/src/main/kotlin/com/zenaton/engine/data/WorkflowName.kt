package com.zenaton.engine.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.engine.data.interfaces.NameInterface
import java.util.UUID

data class WorkflowName
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    constructor(@get:JsonValue override val name: String = UUID.randomUUID().toString()) :
    NameInterface
