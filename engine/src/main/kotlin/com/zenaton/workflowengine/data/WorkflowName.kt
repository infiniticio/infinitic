package com.zenaton.workflowengine.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.commons.data.interfaces.NameInterface
import java.util.UUID

data class WorkflowName
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    constructor(@get:JsonValue override val name: String = UUID.randomUUID().toString()) :
    NameInterface
