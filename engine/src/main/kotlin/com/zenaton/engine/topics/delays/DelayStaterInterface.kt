package com.zenaton.engine.topics.delays

import com.zenaton.engine.data.delays.DelayState

interface DelayStaterInterface {
    fun getState(key: String): DelayState?
    fun createState(state: DelayState)
    fun updateState(state: DelayState)
    fun deleteState(state: DelayState)
}
