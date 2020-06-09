package com.zenaton.decisionmanager.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.commons.data.interfaces.NameInterface

data class DecisionName
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val name: String) :
    NameInterface
