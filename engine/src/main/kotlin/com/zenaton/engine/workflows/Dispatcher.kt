package com.zenaton.engine.workflows

import com.zenaton.engine.decisions.Message.DecisionDispatched
import com.zenaton.engine.tasks.Message.TaskDispatched

interface Dispatcher {
    fun createState(state: State)
    fun updateState(state: State)
    fun deleteState(state: State)
    fun dispatchTask(msg: TaskDispatched)
    fun dispatchDecision(msg: DecisionDispatched)
    fun log()
}
