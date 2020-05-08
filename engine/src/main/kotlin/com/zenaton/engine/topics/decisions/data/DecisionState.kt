package com.zenaton.engine.topics.decisions.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.engine.data.DecisionId
import com.zenaton.engine.data.interfaces.StateInterface

data class DecisionState @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@get:JsonValue val decisionId: DecisionId) : StateInterface
