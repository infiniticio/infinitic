package com.zenaton.decisionmanager.messages.interfaces

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zenaton.decisionmanager.data.DecisionId

interface DecisionMessageInterface {
    val decisionId: DecisionId
    @JsonIgnore fun getStateId() = decisionId.id
}
