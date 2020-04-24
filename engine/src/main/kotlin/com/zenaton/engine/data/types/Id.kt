package com.zenaton.engine.data.types

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

abstract class Id @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@get:JsonValue open val id: String)
