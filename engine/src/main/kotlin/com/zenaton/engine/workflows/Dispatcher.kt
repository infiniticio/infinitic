package com.zenaton.engine.workflows

import com.zenaton.engine.tasks.Message.TaskDispatched
import com.zenaton.engine.decisions.Message.DecisionDispatched

interface Dispatcher {
    fun createState(state: State)
    fun updateState(state: State)
    fun deleteState(state: State)
    fun dispatchTask(msg: TaskDispatched)
    fun dispatchDecision(msg: DecisionDispatched)
    fun log()
}
