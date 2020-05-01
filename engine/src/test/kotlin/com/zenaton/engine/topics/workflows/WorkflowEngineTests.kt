package com.zenaton.engine.topics.workflows

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.decisions.DecisionId
import com.zenaton.engine.data.delays.DelayId
import com.zenaton.engine.data.events.EventData
import com.zenaton.engine.data.events.EventName
import com.zenaton.engine.data.tasks.TaskId
import com.zenaton.engine.data.tasks.TaskOutput
import com.zenaton.engine.data.workflows.WorkflowData
import com.zenaton.engine.data.workflows.WorkflowId
import com.zenaton.engine.data.workflows.WorkflowName
import com.zenaton.engine.data.workflows.WorkflowOutput
import com.zenaton.engine.data.workflows.WorkflowState
import com.zenaton.engine.data.workflows.states.Branch
import com.zenaton.engine.topics.decisions.messages.DecisionDispatched
import com.zenaton.engine.topics.workflows.messages.ChildWorkflowCompleted
import com.zenaton.engine.topics.workflows.messages.DecisionCompleted
import com.zenaton.engine.topics.workflows.messages.DelayCompleted
import com.zenaton.engine.topics.workflows.messages.EventReceived
import com.zenaton.engine.topics.workflows.messages.TaskCompleted
import com.zenaton.engine.topics.workflows.messages.WorkflowDispatched
import com.zenaton.engine.topics.workflows.messages.WorkflowMessageInterface
import com.zenaton.pulsar.topics.workflows.WorkflowDispatcher
import com.zenaton.pulsar.topics.workflows.WorkflowStater
import com.zenaton.pulsar.utils.Logger
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
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

fun decisionCompleted(id: WorkflowId? = null): DecisionCompleted {
    return DecisionCompleted(
        workflowId = id ?: WorkflowId(),
        decisionId = DecisionId()
    )
}

fun taskCompleted(id: WorkflowId? = null, taskId: TaskId? = null, taskOutput: TaskOutput? = null): TaskCompleted {
    return TaskCompleted(
        workflowId = id ?: WorkflowId(),
        taskId = taskId ?: TaskId(),
        taskOutput = taskOutput
    )
}

fun childWorkflowCompleted(id: WorkflowId? = null, childWorkflowId: WorkflowId? = null, childWorkflowOutput: WorkflowOutput? = null): ChildWorkflowCompleted {
    return ChildWorkflowCompleted(
        workflowId = id ?: WorkflowId(),
        childWorkflowId = childWorkflowId ?: WorkflowId(),
        childWorkflowOutput = childWorkflowOutput
    )
}

fun delayCompleted(id: WorkflowId? = null, delayId: DelayId? = null): DelayCompleted {
    return DelayCompleted(
        workflowId = id ?: WorkflowId(),
        delayId = delayId ?: DelayId()
    )
}

fun eventReceived(id: WorkflowId? = null, eventName: EventName? = null, eventData: EventData? = null): EventReceived {
    return EventReceived(
        workflowId = id ?: WorkflowId(),
        eventName = eventName ?: EventName(Arb.string(1).toString()),
        eventData = eventData ?: EventData(Arb.string(1).toString().toByteArray())
    )
}

fun workflowCompleted(id: WorkflowId? = null, taskId: TaskId? = null, taskOutput: TaskOutput? = null): TaskCompleted {
    return TaskCompleted(
        workflowId = id ?: WorkflowId(),
        taskId = taskId ?: TaskId(),
        taskOutput = taskOutput
    )
}

fun shouldWarnAndNothingMoreIfNotState(msgIn: WorkflowMessageInterface) = stringSpec {
    // mocking
    val stater = mockk<WorkflowStater>()
    val dispatcher = mockk<WorkflowDispatcher>()
    val logger = mockk<Logger>()
    every { stater.getState(msgIn.getStateKey()) } returns null
    every { logger.warn(any(), msgIn) } returns "warning!"
    // given
    val engine = WorkflowEngine(stater = stater, dispatcher = dispatcher, logger = logger)
    // when
    engine.handle(msg = msgIn)
    // then
    verify(exactly = 1) { stater.getState(msgIn.getStateKey()) }
    verify(exactly = 1) { logger.warn(any(), msgIn) }
    confirmVerified(stater)
    confirmVerified(dispatcher)
    confirmVerified(logger)
}

fun shouldbufferMessageIfOngoingDecision(msgIn: WorkflowMessageInterface) = stringSpec {
    "Should buffer ${msgIn::class.simpleName} message if there is an ongoing decision" {
        // mocking
        val stater = mockk<WorkflowStater>()
        val dispatcher = mockk<WorkflowDispatcher>()
        val logger = mockk<Logger>()
        val state = WorkflowState(workflowId = msgIn.workflowId, ongoingDecisionId = DecisionId())
        val slotState = slot<WorkflowState>()
        every { stater.getState(msgIn.getStateKey()) } returns state
        every { stater.updateState(state = capture(slotState)) } just Runs
        // given
        val engine = WorkflowEngine(stater = stater, dispatcher = dispatcher, logger = logger)
        // when
        engine.handle(msg = msgIn)
        // then
        val stateOut = slotState.captured
        verify(exactly = 1) { stater.getState(msgIn.getStateKey()) }
        verify(exactly = 1) { stater.updateState(stateOut) }
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
        val stater = mockk<WorkflowStater>()
        val dispatcher = mockk<WorkflowDispatcher>()
        val logger = mockk<Logger>()
        val state = mockk<WorkflowState>()
        every { stater.getState(msgIn.getStateKey()) } returns state
        every { state.workflowId } returns WorkflowId()
        every { logger.error(any(), msgIn, state) } returns "error!"
        // given
        val engine = WorkflowEngine(stater = stater, dispatcher = dispatcher, logger = logger)
        // when
        engine.handle(msg = msgIn)
        // then
        verify(exactly = 1) { logger.error(any(), msgIn, state) }
        verify(exactly = 1) { stater.getState(msgIn.getStateKey()) }
        confirmVerified(stater)
        confirmVerified(dispatcher)
        confirmVerified(logger)
    }

    "Should log error and nothing more if workflowDispatched with existing state" {
        val msgIn = workflowDispatched()
        // mocking
        val stater = mockk<WorkflowStater>()
        val dispatcher = mockk<WorkflowDispatcher>()
        val logger = mockk<Logger>()
        val state = mockk<WorkflowState>()
        every { stater.getState(msgIn.getStateKey()) } returns state
        every { state.workflowId } returns msgIn.workflowId
        every { logger.error(any(), msgIn) } returns "error!"
        // given
        val engine = WorkflowEngine(stater = stater, dispatcher = dispatcher, logger = logger)
        // when
        engine.handle(msg = msgIn)
        // then
        verify(exactly = 1) { stater.getState(msgIn.getStateKey()) }
        verify(exactly = 1) { logger.error(any(), msgIn) }
        confirmVerified(stater)
        confirmVerified(dispatcher)
        confirmVerified(logger)
    }

    "Should log error and nothing more if decisionCompleted inconsistent with state ongoing decision" {
        val msgIn = decisionCompleted()
        // mocking
        val stater = mockk<WorkflowStater>()
        val dispatcher = mockk<WorkflowDispatcher>()
        val logger = mockk<Logger>()
        val state = WorkflowState(workflowId = msgIn.workflowId, ongoingDecisionId = DecisionId())
        every { stater.getState(msgIn.getStateKey()) } returns state
        every { logger.error(any(), msgIn, state) } returns "error!"
        // given
        val engine = WorkflowEngine(stater = stater, dispatcher = dispatcher, logger = logger)
        // when
        engine.handle(msg = msgIn)
        // then
        verify(exactly = 1) { stater.getState(msgIn.getStateKey()) }
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
        val stater = mockk<WorkflowStater>()
        val dispatcher = mockk<WorkflowDispatcher>()
        val logger = mockk<Logger>()
        val slotMsg = slot<DecisionDispatched>()
        val slotKey = slot<String>()
        val slotState = slot<WorkflowState>()
        every { stater.getState(key = capture(slotKey)) } returns null
        every { dispatcher.dispatchDecision(msg = capture(slotMsg)) } just Runs
        every { stater.createState(state = capture(slotState)) } just Runs
        // given
        val msgIn = workflowDispatched()
        val engine = WorkflowEngine(stater = stater, dispatcher = dispatcher, logger = logger)
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
        msgOut.branches shouldBe listOf(Branch.Handle(branchId = msgOut.branches[0].branchId, workflowData = msgIn.workflowData))
        stateOut.ongoingDecisionId shouldBe msgOut.decisionId
        stateOut.runningBranches shouldBe listOf(Branch.Handle(branchId = msgOut.branches[0].branchId, workflowData = msgIn.workflowData))
    }
})
