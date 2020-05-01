package com.zenaton.engine.topics.decisions

import com.zenaton.engine.data.decisions.DecisionState

interface DecisionStaterInterface {
    fun getState(key: String): DecisionState?
    fun createState(state: DecisionState)
    fun updateState(state: DecisionState)
    fun deleteState(state: DecisionState)
}
