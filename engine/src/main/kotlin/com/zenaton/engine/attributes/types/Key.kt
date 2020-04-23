package com.zenaton.engine.attributes.types

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

abstract class Key @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@get:JsonValue open val key: String)
