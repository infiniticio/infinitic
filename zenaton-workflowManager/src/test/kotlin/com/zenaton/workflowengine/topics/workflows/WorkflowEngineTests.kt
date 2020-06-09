package com.zenaton.workflowengine.topics.workflows

import com.zenaton.commons.pulsar.utils.Logger
import com.zenaton.commons.pulsar.utils.StateStorage
import com.zenaton.decisionmanager.data.DecisionId
import com.zenaton.decisionmanager.data.DecisionName
import com.zenaton.decisionmanager.messages.DecisionDispatched
import com.zenaton.workflowengine.data.WorkflowId
import com.zenaton.workflowengine.pulsar.topics.workflows.functions.WorkflowEngineDispatcher
import com.zenaton.workflowengine.topics.workflows.engine.WorkflowEngine
import com.zenaton.workflowengine.topics.workflows.interfaces.WorkflowMessageInterface
import com.zenaton.workflowengine.topics.workflows.messages.ChildWorkflowCompleted
import com.zenaton.workflowengine.topics.workflows.messages.DecisionCompleted
import com.zenaton.workflowengine.topics.workflows.messages.DelayCompleted
import com.zenaton.workflowengine.topics.workflows.messages.EventReceived
import com.zenaton.workflowengine.topics.workflows.messages.TaskCompleted
import com.zenaton.workflowengine.topics.workflows.messages.WorkflowCompleted
import com.zenaton.workflowengine.topics.workflows.messages.WorkflowDispatched
import com.zenaton.workflowengine.topics.workflows.state.WorkflowState
import com.zenaton.workflowengine.utils.TestFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

fun workflowDispatched() = TestFactory.get(WorkflowDispatched::class)
fun decisionCompleted() = TestFactory.get(DecisionCompleted::class)
fun taskCompleted() = TestFactory.get(TaskCompleted::class)
fun childWorkflowCompleted() = TestFactory.get(ChildWorkflowCompleted::class)
fun delayCompleted() = TestFactory.get(DelayCompleted::class)
fun eventReceived() = TestFactory.get(EventReceived::class)
fun workflowCompleted() = TestFactory.get(WorkflowCompleted::class)

fun shouldWarnAndNothingMoreIfNotState(msgIn: WorkflowMessageInterface) = stringSpec {
    // mocking
    val stater = mockk<StateStorage<WorkflowState>>()
    val dispatcher = mockk<WorkflowEngineDispatcher>()
    val logger = mockk<Logger>()
    every { stater.getState(msgIn.getStateId()) } returns null
    every { logger.warn(any(), msgIn) } returns "warning!"
    // given
    val engine = WorkflowEngine(
        stater = stater,
        dispatcher = dispatcher,
        logger = logger
    )
    // when
    engine.handle(msg = msgIn)
    // then
    verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
    verify(exactly = 1) { logger.warn(any(), msgIn) }
    confirmVerified(stater)
    confirmVerified(dispatcher)
    confirmVerified(logger)
}

fun shouldbufferMessageIfOngoingDecision(msgIn: WorkflowMessageInterface) = stringSpec {
    "Should buffer ${msgIn::class.simpleName} message if there is an ongoing decision" {
        // mocking
        val stater = mockk<StateStorage<WorkflowState>>()
        val dispatcher = mockk<WorkflowEngineDispatcher>()
        val logger = mockk<Logger>()
        val state = WorkflowState(
            workflowId = msgIn.workflowId,
            ongoingDecisionId = DecisionId()
        )
        val slotState = slot<WorkflowState>()
        every { stater.getState(msgIn.getStateId()) } returns state
        every { stater.updateState(any(), state = capture(slotState)) } just Runs
        // given
        val engine = WorkflowEngine(
            stater = stater,
            dispatcher = dispatcher,
            logger = logger
        )
        // when
        engine.handle(msg = msgIn)
        // then
        val stateOut = slotState.captured
        verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
        verify(exactly = 1) { stater.updateState(msgIn.getStateId(), stateOut) }
        confirmVerified(stater)
        confirmVerified(dispatcher)
        confirmVerified(logger)
        stateOut.bufferedMessages.size shouldBe 1
        stateOut.bufferedMessages[0] shouldBe msgIn
    }
}

class WorkflowEngineTests : StringSpec({

    "Should log error and nothing more if retrieved state and msg have not the same Id" {
        val msgIn = workflowDispatched()
        // mocking
        val stater = mockk<StateStorage<WorkflowState>>()
        val dispatcher = mockk<WorkflowEngineDispatcher>()
        val logger = mockk<Logger>()
        val state = mockk<WorkflowState>()
        every { stater.getState(msgIn.getStateId()) } returns state
        every { state.workflowId } returns WorkflowId()
        every { logger.error(any(), msgIn, state) } returns "error!"
        // given
        val engine = WorkflowEngine(
            stater = stater,
            dispatcher = dispatcher,
            logger = logger
        )
        // when
        engine.handle(msg = msgIn)
        // then
        verify(exactly = 1) { logger.error(any(), msgIn, state) }
        verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
        confirmVerified(stater)
        confirmVerified(dispatcher)
        confirmVerified(logger)
    }

    "Should log error and nothing more if workflowDispatched with existing state" {
        val msgIn = workflowDispatched()
        // mocking
        val stater = mockk<StateStorage<WorkflowState>>()
        val dispatcher = mockk<WorkflowEngineDispatcher>()
        val logger = mockk<Logger>()
        val state = mockk<WorkflowState>()
        every { stater.getState(msgIn.getStateId()) } returns state
        every { state.workflowId } returns msgIn.workflowId
        every { logger.error(any(), msgIn) } returns "error!"
        // given
        val engine = WorkflowEngine(
            stater = stater,
            dispatcher = dispatcher,
            logger = logger
        )
        // when
        engine.handle(msg = msgIn)
        // then
        verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
        verify(exactly = 1) { logger.error(any(), msgIn) }
        confirmVerified(stater)
        confirmVerified(dispatcher)
        confirmVerified(logger)
    }

    "Should log error and nothing more if decisionCompleted inconsistent with state ongoing decision" {
        val msgIn = decisionCompleted()
        // mocking
        val stater = mockk<StateStorage<WorkflowState>>()
        val dispatcher = mockk<WorkflowEngineDispatcher>()
        val logger = mockk<Logger>()
        val state = WorkflowState(
            workflowId = msgIn.workflowId,
            ongoingDecisionId = DecisionId()
        )
        every { stater.getState(msgIn.getStateId()) } returns state
        every { logger.error(any(), msgIn, state) } returns "error!"
        // given
        val engine = WorkflowEngine(
            stater = stater,
            dispatcher = dispatcher,
            logger = logger
        )
        // when
        engine.handle(msg = msgIn)
        // then
        verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
        verify(exactly = 1) { logger.error(any(), msgIn, state) }
        confirmVerified(stater)
        confirmVerified(dispatcher)
        confirmVerified(logger)
    }

    include(shouldWarnAndNothingMoreIfNotState(decisionCompleted()))
    include(shouldWarnAndNothingMoreIfNotState(taskCompleted()))
    include(shouldWarnAndNothingMoreIfNotState(childWorkflowCompleted()))
    include(shouldWarnAndNothingMoreIfNotState(delayCompleted()))
    include(shouldWarnAndNothingMoreIfNotState(eventReceived()))
    include(shouldWarnAndNothingMoreIfNotState(workflowCompleted()))

    include(shouldbufferMessageIfOngoingDecision(taskCompleted()))
    include(shouldbufferMessageIfOngoingDecision(childWorkflowCompleted()))
    include(shouldbufferMessageIfOngoingDecision(delayCompleted()))
    include(shouldbufferMessageIfOngoingDecision(eventReceived()))

    "Dispatching a workflow" {
        // mocking
        val stater = mockk<StateStorage<WorkflowState>>()
        val dispatcher = mockk<WorkflowEngineDispatcher>()
        val logger = mockk<Logger>()
        val slotMsg = slot<DecisionDispatched>()
        val slotState = slot<WorkflowState>()
        val msgIn = workflowDispatched()
        every { stater.getState(msgIn.getStateId()) } returns null
        every { dispatcher.dispatch(msg = capture(slotMsg)) } just Runs
        every { stater.createState(key = msgIn.getStateId(), state = capture(slotState)) } just Runs
        // given
        val engine = WorkflowEngine(
            stater = stater,
            dispatcher = dispatcher,
            logger = logger
        )
        // when
        engine.handle(msg = msgIn)
        // then
        val msgOut = slotMsg.captured
        val stateOut = slotState.captured
        verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
        verify(exactly = 1) { dispatcher.dispatch(msgOut) }
        verify(exactly = 1) { stater.createState(msgIn.getStateId(), stateOut) }
        confirmVerified(stater)
        confirmVerified(dispatcher)
        confirmVerified(logger)
        msgOut.workflowId shouldBe msgIn.workflowId
        msgOut.decisionName shouldBe DecisionName(msgIn.workflowName.name)
//        msgOut.decisionData shouldBe listOf(Branch.Handle(branchId = msgOut.branches[0].branchId, workflowData = msgIn.workflowData))
        stateOut.ongoingDecisionId shouldBe msgOut.decisionId
//        stateOut.runningBranches shouldBe listOf(Branch.Handle(branchId = msgOut.branches[0].branchId, workflowData = msgIn.workflowData))
    }
})
