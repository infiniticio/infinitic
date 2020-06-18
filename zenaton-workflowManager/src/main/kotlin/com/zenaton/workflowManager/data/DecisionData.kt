package com.zenaton.workflowManager.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.commons.data.SerializedData

data class DecisionData
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue val input: List<SerializedData>)
