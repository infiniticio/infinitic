package com.zenaton.taskmanager.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.engine.data.interfaces.NameInterface

data class TaskName
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    constructor(@get:JsonValue override val name: String) :
    NameInterface
