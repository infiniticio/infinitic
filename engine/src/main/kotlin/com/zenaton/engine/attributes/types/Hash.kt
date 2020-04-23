package com.zenaton.engine.attributes.types

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

abstract class Hash @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@get:JsonValue open val hash: String)
