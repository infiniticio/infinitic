package com.zenaton.engine.workflows

import com.zenaton.engine.common.attributes.WorkflowId
import com.zenaton.engine.workflows.Message as WorkflowMessage
import com.zenaton.engine.tasks.Message.TaskDispatched
import com.zenaton.engine.decisions.Message.DecisionDispatched
import com.zenaton.engine.workflows.Message.WorkflowDispatched

sealed class EngineAction {
    data class CreateState(val state: State)
    data class UpdateState(val state: State)
    data class DeleteState(val state: State)
    data class DispatchTask(val msg: TaskDispatched)
    data class DispatchDecision(val msg: DecisionDispatched)
}

data class EngineStep(
    val stateIn: State?,
    val msgIn: WorkflowMessage,
    val out: List<EngineAction>?
)

enum class EngineScenario(listOf: List<EngineStep>) {
    SINGLE_TASK_WORKFLOW(listOf(
        EngineStep(
            stateIn = null,
            msgIn = WorkflowDispatched(WorkflowId(), "test", workflowData = ""),
            out = listOf(
//                EngineAction.CreateState(State())
            )
        )
    ))
}
/*
EngineTests

In:
    workflow.state0
    workflows.Messages0
Out:
    workflow.state1
    decision.Messages1

In:
    workflow.state1
    workflows.Messages1
Out:
    workflow.state2
    decision.Messages2
*/

/*
DecisionTests

In:
    decision.Messages1
Out:
    workflows.Messages1

In:
    decision.Message2
Out:
    workflows.Messages2
*/
