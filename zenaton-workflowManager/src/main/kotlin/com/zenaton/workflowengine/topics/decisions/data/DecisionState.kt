package com.zenaton.workflowengine.topics.decisions.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.decisionmanager.data.DecisionId

data class DecisionState @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@get:JsonValue val decisionId: DecisionId) : StateInterface
