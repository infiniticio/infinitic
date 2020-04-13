package com.zenaton.engine.workflows

import com.zenaton.engine.common.attributes.WorkflowId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

fun workflowDispatched(id: WorkflowId?, workflowData: String?, workflowName: String?): Message.WorkflowDispatched {
    return Message.WorkflowDispatched(
        id ?: WorkflowId(),
        workflowData ?: Arb.string(1).toString(),
        workflowName ?: Arb.string(1).toString()
    )
}

class EngineTests : StringSpec({
    "Dispatching a workflow" {
        // mocking
        val dispatcher = mockk<Dispatcher>()
        val slotMsg = slot<com.zenaton.engine.decisions.Message.DecisionDispatched>()
        val slotState = slot<State>()
        every { dispatcher.dispatchDecision(msg = capture(slotMsg)) } just Runs
        every { dispatcher.createState(state = capture(slotState)) } just Runs
        // given
        val stateIn = null
        val msgIn = workflowDispatched(null, null, null)
        val engine = Engine(state = stateIn, msg = msgIn, dispatcher = dispatcher)
        // when
        engine.handle()
        // then
        val msgOut = slotMsg.captured
        val stateOut = slotState.captured
        verify(exactly = 1) { dispatcher.dispatchDecision(msgOut) }
        verify(exactly = 1) { dispatcher.createState(stateOut) }
        confirmVerified(dispatcher)
        msgOut.workflowId shouldBe msgIn.workflowId
        msgOut.workflowName shouldBe msgIn.workflowName
    }
})
