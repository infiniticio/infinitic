package com.zenaton.decisionmanager.messages.interfaces

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zenaton.commons.data.DateTime
import com.zenaton.decisionmanager.data.DecisionId

interface DecisionMessageInterface {
    val decisionId: DecisionId
    var sentAt: DateTime?
    @JsonIgnore fun getStateId() = decisionId.id
}
