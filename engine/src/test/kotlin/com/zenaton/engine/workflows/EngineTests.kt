package com.zenaton.engine.workflows

import com.zenaton.engine.common.attributes.WorkflowId
import com.zenaton.engine.decisions.Message.DecisionDispatched
import com.zenaton.engine.workflows.messages.WorkflowDispatched
import com.zenaton.pulsar.workflows.Dispatcher
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

fun workflowDispatched(id: WorkflowId? = null, workflowData: String? = null, workflowName: String? = null): WorkflowDispatched {
    return WorkflowDispatched(
        id ?: WorkflowId(),
        workflowData ?: Arb.string(1).toString(),
        workflowName ?: Arb.string(1).toString()
    )
}

class EngineTests : StringSpec({
    "Dispatching a workflow" {
        // mocking
        val dispatcher = mockk<Dispatcher>()
        val slotMsg = slot<DecisionDispatched>()
        val slotKey = slot<String>()
        val slotState = slot<WorkflowState>()
        every { dispatcher.getState(key = capture(slotKey)) } returns null
        every { dispatcher.dispatchDecision(msg = capture(slotMsg)) } just Runs
        every { dispatcher.updateState(state = capture(slotState)) } just Runs
        // given
        val msgIn = workflowDispatched()
        val engine = Engine(dispatcher = dispatcher, msg = msgIn)
        // when
        engine.handle()
        // then
        val keyOut = slotKey.captured
        val msgOut = slotMsg.captured
        val stateOut = slotState.captured
        verify(exactly = 1) { dispatcher.getState(keyOut) }
        verify(exactly = 1) { dispatcher.dispatchDecision(msgOut) }
        verify(exactly = 1) { dispatcher.updateState(stateOut) }
        confirmVerified(dispatcher)
        msgOut.workflowId shouldBe msgIn.workflowId
        msgOut.workflowName shouldBe msgIn.workflowName
    }
})
