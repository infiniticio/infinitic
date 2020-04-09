package com.zenaton.engine.workflows

import com.zenaton.engine.workflows.state.State

interface Dispatcher {
    fun createState(state: State)
    fun storeState(state: State)
    fun deleteState(state: State)
    fun dispatchTask()
    fun dispatchDecision()
}
