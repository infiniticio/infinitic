package com.zenaton.engine.workflows

import com.zenaton.engine.attributes.delays.DelayId
import com.zenaton.engine.attributes.tasks.TaskId
import com.zenaton.engine.attributes.tasks.TaskOutput
import com.zenaton.engine.attributes.types.DateTime
import com.zenaton.engine.attributes.workflows.WorkflowData
import com.zenaton.engine.attributes.workflows.WorkflowId
import com.zenaton.engine.attributes.workflows.WorkflowName
import com.zenaton.engine.attributes.workflows.WorkflowState
import com.zenaton.engine.decisions.DecisionDispatched
import com.zenaton.pulsar.workflows.Dispatcher
import com.zenaton.pulsar.workflows.Logger
import com.zenaton.pulsar.workflows.Stater
import io.kotest.assertions.throwables.shouldThrow
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

fun workflowDispatched(id: WorkflowId? = null, workflowData: WorkflowData? = null, workflowName: WorkflowName? = null): WorkflowDispatched {
    return WorkflowDispatched(
        workflowId = id ?: WorkflowId(),
        workflowName = workflowName ?: WorkflowName(
            Arb.string(1).toString()
        ),
        workflowData = workflowData ?: WorkflowData(
            Arb.string(1).toString().toByteArray()
        ),
        dispatchedAt = DateTime()
    )
}

fun decisionCompleted(id: WorkflowId? = null, workflowData: String? = null, workflowName: String? = null): DecisionCompleted {
    return DecisionCompleted(
        id ?: WorkflowId()
    )
}

fun taskCompleted(id: WorkflowId? = null, taskId: TaskId? = null, taskOutput: TaskOutput? = null): TaskCompleted {
    return TaskCompleted(
        workflowId = id ?: WorkflowId(),
        taskId = taskId ?: TaskId(),
        taskOutput = taskOutput
    )
}

fun delayCompleted(id: WorkflowId? = null, delayId: DelayId? = null): DelayCompleted {
    return DelayCompleted(
        id ?: WorkflowId(),
        delayId ?: DelayId()
    )
}

class EngineTests : StringSpec({

    "Should throw exception if retrieved state and msg have not the same Id" {
        val msgIn = workflowDispatched()
        // mocking
        val stater = mockk<Stater>()
        val dispatcher = mockk<Dispatcher>()
        val logger = mockk<Logger>()
        val state = mockk<WorkflowState>()
        every { stater.getState(msgIn.getStateKey()) } returns state
        every { state.workflowId } returns WorkflowId()
        every { logger.error(any(), msgIn) } returns "error!"
        // given
        val engine = Engine(stater = stater, dispatcher = dispatcher, logger = logger)
        // when, then
        shouldThrow<Exception> {
            engine.handle(msg = msgIn)
        }
        verify(exactly = 1) { logger.error(any(), msgIn) }
        verify(exactly = 1) { stater.getState(msgIn.getStateKey()) }
        confirmVerified(stater)
        confirmVerified(dispatcher)
        confirmVerified(logger)
    }

    "workflowDispatched with existing state should do nothing except logging" {
        val msgIn = workflowDispatched()
        // mocking
        val stater = mockk<Stater>()
        val dispatcher = mockk<Dispatcher>()
        val logger = mockk<Logger>()
        val state = mockk<WorkflowState>()
        every { stater.getState(msgIn.getStateKey()) } returns state
        every { state.workflowId } returns msgIn.workflowId
        every { logger.warn(any(), msgIn) } returns "warning!"
        // given
        val engine = Engine(stater = stater, dispatcher = dispatcher, logger = logger)
        // when
        engine.handle(msg = msgIn)
        // then
        verify(exactly = 1) { stater.getState(msgIn.getStateKey()) }
        verify(exactly = 1) { logger.warn(any(), msgIn) }
        confirmVerified(stater)
        confirmVerified(dispatcher)
        confirmVerified(logger)
    }

    "DecisionCompleted without existing state should do nothing except logging" {
        val msgIn = decisionCompleted()
        // mocking
        val stater = mockk<Stater>()
        val dispatcher = mockk<Dispatcher>()
        val logger = mockk<Logger>()
        every { stater.getState(msgIn.getStateKey()) } returns null
        every { logger.warn(any(), msgIn) } returns "warning!"
        // given
        val engine = Engine(stater = stater, dispatcher = dispatcher, logger = logger)
        // when
        engine.handle(msg = msgIn)
        // then
        verify(exactly = 1) { stater.getState(msgIn.getStateKey()) }
        verify(exactly = 1) { logger.warn(any(), msgIn) }
        confirmVerified(stater)
        confirmVerified(dispatcher)
        confirmVerified(logger)
    }

    "TaskCompleted without existing state should do nothing except logging" {
        val msgIn = taskCompleted()
        // mocking
        val stater = mockk<Stater>()
        val dispatcher = mockk<Dispatcher>()
        val logger = mockk<Logger>()
        every { stater.getState(msgIn.getStateKey()) } returns null
        every { logger.warn(any(), msgIn) } returns "warning!"
        // given
        val engine = Engine(stater = stater, dispatcher = dispatcher, logger = logger)
        // when
        engine.handle(msg = msgIn)
        // then
        verify(exactly = 1) { stater.getState(msgIn.getStateKey()) }
        verify(exactly = 1) { logger.warn(any(), msgIn) }
        confirmVerified(stater)
        confirmVerified(dispatcher)
        confirmVerified(logger)
    }

    "DelayCompleted without existing state should do nothing except logging" {
        val msgIn = delayCompleted()
        // mocking
        val stater = mockk<Stater>()
        val dispatcher = mockk<Dispatcher>()
        val logger = mockk<Logger>()
        every { stater.getState(msgIn.getStateKey()) } returns null
        every { logger.warn(any(), msgIn) } returns "warning!"
        // given
        val engine = Engine(stater = stater, dispatcher = dispatcher, logger = logger)
        // when
        engine.handle(msg = msgIn)
        // then
        verify(exactly = 1) { stater.getState(msgIn.getStateKey()) }
        verify(exactly = 1) { logger.warn(any(), msgIn) }
        confirmVerified(stater)
        confirmVerified(dispatcher)
        confirmVerified(logger)
    }

    "Dispatching a workflow" {
        // mocking
        val stater = mockk<Stater>()
        val dispatcher = mockk<Dispatcher>()
        val logger = mockk<Logger>()
        val slotMsg = slot<DecisionDispatched>()
        val slotKey = slot<String>()
        val slotState = slot<WorkflowState>()
        every { stater.getState(key = capture(slotKey)) } returns null
        every { dispatcher.dispatchDecision(msg = capture(slotMsg)) } just Runs
        every { stater.createState(state = capture(slotState)) } just Runs
        // given
        val msgIn = workflowDispatched()
        val engine = Engine(stater = stater, dispatcher = dispatcher, logger = logger)
        // when
        engine.handle(msg = msgIn)
        // then
        val keyOut = slotKey.captured
        val msgOut = slotMsg.captured
        val stateOut = slotState.captured
        verify(exactly = 1) { stater.getState(keyOut) }
        verify(exactly = 1) { dispatcher.dispatchDecision(msgOut) }
        verify(exactly = 1) { stater.createState(stateOut) }
        confirmVerified(stater)
        confirmVerified(dispatcher)
        confirmVerified(logger)
        msgOut.workflowId shouldBe msgIn.workflowId
        msgOut.workflowName shouldBe msgIn.workflowName
    }
})
