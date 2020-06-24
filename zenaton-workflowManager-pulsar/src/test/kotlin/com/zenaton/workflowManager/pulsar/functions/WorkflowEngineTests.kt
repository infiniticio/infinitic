package com.zenaton.workflowManager.pulsar.functions


import com.zenaton.workflowManager.data.WorkflowId
import com.zenaton.workflowManager.functions.WorkflowEngineFunction
import com.zenaton.workflowManager.engine.WorkflowEngine
import com.zenaton.workflowManager.messages.ChildWorkflowCompleted
import com.zenaton.workflowManager.messages.DecisionCompleted
import com.zenaton.workflowManager.messages.DelayCompleted
import com.zenaton.workflowManager.messages.EventReceived
import com.zenaton.workflowManager.messages.TaskCompleted
import com.zenaton.workflowManager.messages.WorkflowCompleted
import com.zenaton.workflowManager.engine.WorkflowEngineState
import com.zenaton.workflowManager.messages.DecisionDispatched
import com.zenaton.workflowManager.messages.WorkflowCanceled
import com.zenaton.workflowManager.pulsar.utils.TestFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

fun decisionDispatched() = TestFactory.random(DecisionDispatched::class)
fun decisionCompleted() = TestFactory.random(DecisionCompleted::class)
fun taskCompleted() = TestFactory.random(TaskCompleted::class)
fun childWorkflowCompleted() = TestFactory.random(ChildWorkflowCompleted::class)
fun delayCompleted() = TestFactory.random(DelayCompleted::class)
fun eventReceived() = TestFactory.random(EventReceived::class)
fun workflowCompleted() = TestFactory.random(WorkflowCompleted::class)
fun workflowCanceled() = TestFactory.random(WorkflowCanceled::class)

//fun shouldbufferMessageIfOngoingDecision(msgIn: WorkflowMessageInterface) = stringSpec {
//    "Should buffer ${msgIn::class.simpleName} message if there is an ongoing decision" {
//        // mocking
//        val stater = mockk<StateStorage<WorkflowEngineState>>()
//        val dispatcher = mockk<WorkflowEngineDispatcher>()
//        val logger = mockk<Logger>()
//        val state = WorkflowEngineState(
//            workflowId = msgIn.workflowId,
//            ongoingDecisionId = DecisionId()
//        )
//        val slotState = slot<WorkflowEngineState>()
//        every { stater.getState(msgIn.getStateId()) } returns state
//        every { stater.updateState(any(), state = capture(slotState)) } just Runs
//        // given
//        val engine = WorkflowEngine(
//            stater = stater,
//            dispatcher = dispatcher,
//            logger = logger
//        )
//        // when
//        engine.handle(msg = msgIn)
//        // then
//        val stateOut = slotState.captured
//        verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
//        verify(exactly = 1) { stater.updateState(msgIn.getStateId(), stateOut) }
//        confirmVerified(stater)
//        confirmVerified(dispatcher)
//        confirmVerified(logger)
//        stateOut.bufferedMessages.size shouldBe 1
//        stateOut.bufferedMessages[0] shouldBe msgIn
//    }
//}

class WorkflowEngineTests : StringSpec({
//
//    "Should log error and nothing more if retrieved state and msg have not the same Id" {
//        val msgIn = workflowDispatched()
//        // mocking
//        val stater = mockk<StateStorage<WorkflowEngineState>>()
//        val dispatcher = mockk<WorkflowEngineDispatcher>()
//        val logger = mockk<Logger>()
//        val state = mockk<WorkflowEngineState>()
//        every { stater.getState(msgIn.getStateId()) } returns state
//        every { state.workflowId } returns WorkflowId()
//        every { logger.error(any(), msgIn, state) } returns "error!"
//        // given
//        val engine = WorkflowEngine(
//            stater = stater,
//            dispatcher = dispatcher,
//            logger = logger
//        )
//        // when
//        engine.handle(msg = msgIn)
//        // then
//        verify(exactly = 1) { logger.error(any(), msgIn, state) }
//        verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
//        confirmVerified(stater)
//        confirmVerified(dispatcher)
//        confirmVerified(logger)
//    }
//
//    "Should log error and nothing more if workflowDispatched with existing state" {
//        val msgIn = workflowDispatched()
//        // mocking
//        val stater = mockk<StateStorage<WorkflowEngineState>>()
//        val dispatcher = mockk<WorkflowEngineDispatcher>()
//        val logger = mockk<Logger>()
//        val state = mockk<WorkflowEngineState>()
//        every { stater.getState(msgIn.getStateId()) } returns state
//        every { state.workflowId } returns msgIn.workflowId
//        every { logger.error(any(), msgIn) } returns "error!"
//        // given
//        val engine = WorkflowEngine(
//            stater = stater,
//            dispatcher = dispatcher,
//            logger = logger
//        )
//        // when
//        engine.handle(msg = msgIn)
//        // then
//        verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
//        verify(exactly = 1) { logger.error(any(), msgIn) }
//        confirmVerified(stater)
//        confirmVerified(dispatcher)
//        confirmVerified(logger)
//    }
//
//    "Should log error and nothing more if decisionCompleted inconsistent with state ongoing decision" {
//        val msgIn = decisionCompleted()
//        // mocking
//        val stater = mockk<StateStorage<WorkflowEngineState>>()
//        val dispatcher = mockk<WorkflowEngineDispatcher>()
//        val logger = mockk<Logger>()
//        val state = WorkflowEngineState(
//            workflowId = msgIn.workflowId,
//            ongoingDecisionId = DecisionId()
//        )
//        every { stater.getState(msgIn.getStateId()) } returns state
//        every { logger.error(any(), msgIn, state) } returns "error!"
//        // given
//        val engine = WorkflowEngine(
//            stater = stater,
//            dispatcher = dispatcher,
//            logger = logger
//        )
//        // when
//        engine.handle(msg = msgIn)
//        // then
//        verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
//        verify(exactly = 1) { logger.error(any(), msgIn, state) }
//        confirmVerified(stater)
//        confirmVerified(dispatcher)
//        confirmVerified(logger)
//    }
//
//    include(
//        shouldWarnAndNothingMoreIfNotState(
//            decisionCompleted()
//        )
//    )
//    include(
//        shouldWarnAndNothingMoreIfNotState(
//            taskCompleted()
//        )
//    )
//    include(
//        shouldWarnAndNothingMoreIfNotState(
//            childWorkflowCompleted()
//        )
//    )
//    include(
//        shouldWarnAndNothingMoreIfNotState(
//            delayCompleted()
//        )
//    )
//    include(
//        shouldWarnAndNothingMoreIfNotState(
//            eventReceived()
//        )
//    )
//    include(
//        shouldWarnAndNothingMoreIfNotState(
//            workflowCompleted()
//        )
//    )
//
//    include(
//        shouldbufferMessageIfOngoingDecision(
//            taskCompleted()
//        )
//    )
//    include(
//        shouldbufferMessageIfOngoingDecision(
//            childWorkflowCompleted()
//        )
//    )
//    include(
//        shouldbufferMessageIfOngoingDecision(
//            delayCompleted()
//        )
//    )
//    include(
//        shouldbufferMessageIfOngoingDecision(
//            eventReceived()
//        )
//    )
//
//    "Dispatching a workflow" {
//        // mocking
//        val stater = mockk<StateStorage<WorkflowEngineState>>()
//        val dispatcher = mockk<WorkflowEngineDispatcher>()
//        val logger = mockk<Logger>()
//        val slotMsg = slot<DecisionDispatched>()
//        val slotState = slot<WorkflowEngineState>()
//        val msgIn = workflowDispatched()
//        every { stater.getState(msgIn.getStateId()) } returns null
//        every { dispatcher.dispatch(msg = capture(slotMsg)) } just Runs
//        every { stater.createState(key = msgIn.getStateId(), state = capture(slotState)) } just Runs
//        // given
//        val engine = WorkflowEngine(
//            stater = stater,
//            dispatcher = dispatcher,
//            logger = logger
//        )
//        // when
//        engine.handle(msg = msgIn)
//        // then
//        val msgOut = slotMsg.captured
//        val stateOut = slotState.captured
//        verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
//        verify(exactly = 1) { dispatcher.dispatch(msgOut) }
//        verify(exactly = 1) { stater.createState(msgIn.getStateId(), stateOut) }
//        confirmVerified(stater)
//        confirmVerified(dispatcher)
//        confirmVerified(logger)
//        msgOut.workflowId shouldBe msgIn.workflowId
//        msgOut.decisionName shouldBe DecisionName(msgIn.workflowName.name)
////        msgOut.decisionData shouldBe listOf(Branch.Handle(branchId = msgOut.branches[0].branchId, workflowData = msgIn.workflowData))
//        stateOut.ongoingDecisionId shouldBe msgOut.decisionId
////        stateOut.runningBranches shouldBe listOf(Branch.Handle(branchId = msgOut.branches[0].branchId, workflowData = msgIn.workflowData))
//    }
})
