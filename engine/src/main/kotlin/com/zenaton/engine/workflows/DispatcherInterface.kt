package com.zenaton.engine.workflows

import com.zenaton.engine.decisions.DecisionDispatched
import com.zenaton.engine.tasks.Message.TaskDispatched

interface DispatcherInterface {
    fun dispatchTask(msg: TaskDispatched)
    fun dispatchDecision(msg: DecisionDispatched)
}
