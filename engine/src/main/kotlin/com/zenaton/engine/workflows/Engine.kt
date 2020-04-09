package com.zenaton.engine.workflows

import com.zenaton.engine.workflows.messages.DecisionCompleted
import com.zenaton.engine.workflows.messages.TaskCompleted
import com.zenaton.engine.workflows.messages.WorkflowDispatched
import com.zenaton.engine.workflows.state.State

class Engine (val state: State?, val dispatcher: Dispatcher) {

    fun handleWorkflowDispatched(msg: WorkflowDispatched) {

    }

    fun handleDecisionCompleted(msg: DecisionCompleted) {

    }

    fun handleTaskCompleted(msg: TaskCompleted) {

    }
}
