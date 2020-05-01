package com.zenaton.engine.data.workflows

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class WorkflowId @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@get:JsonValue val id: String = UUID.randomUUID().toString())
