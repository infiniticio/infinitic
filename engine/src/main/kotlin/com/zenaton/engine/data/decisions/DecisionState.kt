package com.zenaton.engine.data.decisions

data class DecisionState(val decisionId: DecisionId) {
    fun getKey() = decisionId.id
}
