package com.zenaton.taskmanager.engine

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.messages.TaskAttemptMessageInterface
import com.zenaton.taskmanager.messages.TaskMessageInterface
import com.zenaton.taskmanager.messages.commands.CancelTask
import com.zenaton.taskmanager.messages.commands.DispatchTask
import com.zenaton.taskmanager.messages.commands.RetryTask
import com.zenaton.taskmanager.messages.commands.RetryTaskAttempt
import com.zenaton.taskmanager.messages.commands.RunTask
import com.zenaton.taskmanager.messages.events.TaskAttemptCompleted
import com.zenaton.taskmanager.messages.events.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.events.TaskAttemptFailed
import com.zenaton.taskmanager.messages.events.TaskAttemptStarted
import com.zenaton.taskmanager.messages.events.TaskCanceled
import com.zenaton.taskmanager.pulsar.dispatcher.TaskDispatcher
import com.zenaton.taskmanager.pulsar.logger.TaskLogger
import com.zenaton.taskmanager.pulsar.state.TaskStater
import com.zenaton.taskmanager.state.TaskState
import com.zenaton.workflowengine.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
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
import io.mockk.verifyOrder

fun cancelTask() = TestFactory.get(CancelTask::class)
fun dispatchTask() = TestFactory.get(DispatchTask::class)
fun retryTask() = TestFactory.get(RetryTask::class)
fun retryTaskAttempt() = TestFactory.get(RetryTaskAttempt::class)
fun runTask() = TestFactory.get(RunTask::class)

fun taskAttemptCompleted() = TestFactory.get(TaskAttemptCompleted::class)
fun taskAttemptDispatched() = TestFactory.get(TaskAttemptDispatched::class)
fun taskAttemptFailed() = TestFactory.get(TaskAttemptFailed::class)
fun taskAttemptStarted() = TestFactory.get(TaskAttemptStarted::class)
fun taskCanceled() = TestFactory.get(TaskCanceled::class)

class TaskEngineTests : StringSpec({
    include(shouldWarnIfNotState(cancelTask()))
    include(shouldWarnIfNotState(retryTask()))
    include(shouldWarnIfNotState(retryTaskAttempt()))
    include(shouldWarnIfNotState(runTask()))
    include(shouldWarnIfNotState(taskAttemptCompleted()))
    include(shouldWarnIfNotState(taskAttemptDispatched()))
    include(shouldWarnIfNotState(taskAttemptFailed()))
    include(shouldWarnIfNotState(taskAttemptStarted()))
    include(shouldWarnIfNotState(taskCanceled()))

    include(shouldErrorIfStateAndMessageHaveInconsistentId(cancelTask()))
    include(shouldErrorIfStateAndMessageHaveInconsistentId(retryTask()))
    include(shouldErrorIfStateAndMessageHaveInconsistentId(retryTaskAttempt()))
    include(shouldErrorIfStateAndMessageHaveInconsistentId(runTask()))
    include(shouldErrorIfStateAndMessageHaveInconsistentId(taskAttemptCompleted()))
    include(shouldErrorIfStateAndMessageHaveInconsistentId(taskAttemptDispatched()))
    include(shouldErrorIfStateAndMessageHaveInconsistentId(taskAttemptFailed()))
    include(shouldErrorIfStateAndMessageHaveInconsistentId(taskAttemptStarted()))
    include(shouldErrorIfStateAndMessageHaveInconsistentId(taskCanceled()))

    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(retryTaskAttempt()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(taskAttemptCompleted()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(taskAttemptDispatched()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(taskAttemptFailed()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(taskAttemptStarted()))

    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(retryTaskAttempt()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(taskAttemptCompleted()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(taskAttemptDispatched()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(taskAttemptFailed()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(taskAttemptStarted()))

    "Should error if task dispatched with existing state" {
        val msgIn = dispatchTask()
        // mocking
        val taskDispatcher = mockk<TaskDispatcher>()
        val workflowDispatcher = mockk<WorkflowDispatcher>()
        val stater = mockk<TaskStater>()
        val logger = mockk<TaskLogger>()
        val state = mockk<TaskState>()
        every { stater.getState(msgIn.getStateId()) } returns state
        every { state.taskId } returns msgIn.taskId
        every { logger.error(any(), msgIn) } returns "error!"
        // given
        val engine = TaskEngine()
        engine.taskDispatcher = taskDispatcher
        engine.workflowDispatcher = workflowDispatcher
        engine.stater = stater
        engine.logger = logger
        // when
        engine.handle(msg = msgIn)
        // then
        verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
        verify(exactly = 1) { logger.error(any(), msgIn) }
        confirmVerified(taskDispatcher)
        confirmVerified(workflowDispatcher)
        confirmVerified(stater)
        confirmVerified(logger)
    }

    "Cancel Task" {
        val msgIn = cancelTask()
        // mocking
        val taskDispatcher = mockk<TaskDispatcher>()
        val workflowDispatcher = mockk<WorkflowDispatcher>()
        val stater = mockk<TaskStater>()
        val logger = mockk<TaskLogger>()
        val state = mockk<TaskState>()
        every { stater.getState(msgIn.getStateId()) } returns state
        every { state.taskId } returns msgIn.taskId
        every { stater.deleteState(any()) } just Runs
        val msgOutSlot = slot<TaskMessageInterface>()
        every { taskDispatcher.dispatch(capture(msgOutSlot)) } just Runs
        // given
        val engine = TaskEngine()
        engine.taskDispatcher = taskDispatcher
        engine.workflowDispatcher = workflowDispatcher
        engine.stater = stater
        engine.logger = logger
        // when
        engine.handle(msg = msgIn)
        // then
        verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
        verify(exactly = 1) { stater.deleteState(msgIn.getStateId()) }
        verify(exactly = 1) { taskDispatcher.dispatch(msgOutSlot.captured) }
        (msgOutSlot.captured is TaskCanceled) shouldBe true
        msgOutSlot.captured.taskId shouldBe msgIn.taskId
        confirmVerified(taskDispatcher)
        confirmVerified(workflowDispatcher)
        confirmVerified(stater)
        confirmVerified(logger)
    }

    "Dispatch Task" {
        val msgIn = dispatchTask()
        // mocking
        val taskDispatcher = mockk<TaskDispatcher>()
        val workflowDispatcher = mockk<WorkflowDispatcher>()
        val stater = mockk<TaskStater>()
        val logger = mockk<TaskLogger>()
        val stateSlot = slot<TaskState>()
        val msgOutSlot1 = slot<RunTask>()
        val msgOutSlot2 = slot<TaskAttemptDispatched>()
        every { stater.getState(msgIn.getStateId()) } returns null
        every { stater.createState(any(), capture(stateSlot)) } just Runs
        every { taskDispatcher.dispatch(capture(msgOutSlot1)) } just Runs
        every { taskDispatcher.dispatch(capture(msgOutSlot2)) } just Runs
        // given
        val engine = TaskEngine()
        engine.taskDispatcher = taskDispatcher
        engine.workflowDispatcher = workflowDispatcher
        engine.stater = stater
        engine.logger = logger
        // when
        engine.handle(msg = msgIn)
        // then
        val runTask = msgOutSlot1.captured as RunTask
        val taskAttemptDispatched = msgOutSlot2.captured
        val state = stateSlot.captured
        verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
        verify(exactly = 1) { stater.createState(msgIn.getStateId(), state) }
        verifyOrder {
            taskDispatcher.dispatch(runTask)
            taskDispatcher.dispatch(taskAttemptDispatched)
        }
        confirmVerified(taskDispatcher)
        confirmVerified(workflowDispatcher)
        confirmVerified(stater)
        confirmVerified(logger)
        runTask.taskId shouldBe msgIn.taskId
        runTask.taskName shouldBe msgIn.taskName
        runTask.taskData shouldBe msgIn.taskData
        runTask.taskAttemptIndex shouldBe 0
        runTask.taskAttemptId shouldBe taskAttemptDispatched.taskAttemptId
        runTask.taskAttemptIndex shouldBe taskAttemptDispatched.taskAttemptIndex
        state.taskId shouldBe msgIn.taskId
        state.taskName shouldBe msgIn.taskName
        state.taskData shouldBe msgIn.taskData
        state.taskAttemptId shouldBe runTask.taskAttemptId
        state.taskAttemptIndex shouldBe 0
        state.workflowId shouldBe msgIn.workflowId
    }
})

fun shouldWarnIfNotState(msgIn: TaskMessageInterface) = stringSpec {
    // mocking
    val taskDispatcher = mockk<TaskDispatcher>()
    val workflowDispatcher = mockk<WorkflowDispatcher>()
    val stater = mockk<TaskStater>()
    val logger = mockk<TaskLogger>()
    every { stater.getState(msgIn.getStateId()) } returns null
    every { logger.warn(any(), msgIn) } returns "warning!"
    // given
    val engine = TaskEngine()
    engine.taskDispatcher = taskDispatcher
    engine.workflowDispatcher = workflowDispatcher
    engine.stater = stater
    engine.logger = logger
    // when
    engine.handle(msg = msgIn)
    // then
    verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
    verify(exactly = 1) { logger.warn(any(), msgIn) }
    confirmVerified(taskDispatcher)
    confirmVerified(workflowDispatcher)
    confirmVerified(stater)
    confirmVerified(logger)
}

fun shouldErrorIfStateAndMessageHaveInconsistentId(msgIn: TaskMessageInterface) = stringSpec {
    // mocking
    val taskDispatcher = mockk<TaskDispatcher>()
    val workflowDispatcher = mockk<WorkflowDispatcher>()
    val stater = mockk<TaskStater>()
    val logger = mockk<TaskLogger>()
    val state = mockk<TaskState>()
    every { stater.getState(msgIn.getStateId()) } returns state
    every { state.taskId } returns TaskId()
    every { logger.error(any(), msgIn, state) } returns "error!"
    // given
    val engine = TaskEngine()
    engine.taskDispatcher = taskDispatcher
    engine.workflowDispatcher = workflowDispatcher
    engine.stater = stater
    engine.logger = logger
    // when
    engine.handle(msg = msgIn)
    // then
    verify(exactly = 1) { logger.error(any(), msgIn, state) }
    verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
    confirmVerified(taskDispatcher)
    confirmVerified(workflowDispatcher)
    confirmVerified(stater)
    confirmVerified(logger)
}

fun shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(msgIn: TaskAttemptMessageInterface) = stringSpec {
    // mocking
    val taskDispatcher = mockk<TaskDispatcher>()
    val workflowDispatcher = mockk<WorkflowDispatcher>()
    val stater = mockk<TaskStater>()
    val logger = mockk<TaskLogger>()
    val state = mockk<TaskState>()
    every { stater.getState(msgIn.getStateId()) } returns state
    every { state.taskId } returns msgIn.taskId
    every { state.taskAttemptId } returns TaskAttemptId()
    every { logger.warn(any(), msgIn, state) } returns "warning!"
    // given
    val engine = TaskEngine()
    engine.taskDispatcher = taskDispatcher
    engine.workflowDispatcher = workflowDispatcher
    engine.stater = stater
    engine.logger = logger
    // when
    engine.handle(msg = msgIn)
    // then
    verify(exactly = 1) { logger.warn(any(), msgIn, state) }
    verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
    confirmVerified(taskDispatcher)
    confirmVerified(workflowDispatcher)
    confirmVerified(stater)
    confirmVerified(logger)
}

fun shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(msgIn: TaskAttemptMessageInterface) = stringSpec {
    // mocking
    val taskDispatcher = mockk<TaskDispatcher>()
    val workflowDispatcher = mockk<WorkflowDispatcher>()
    val stater = mockk<TaskStater>()
    val logger = mockk<TaskLogger>()
    val state = mockk<TaskState>()
    every { stater.getState(msgIn.getStateId()) } returns state
    every { state.taskId } returns msgIn.taskId
    every { state.taskAttemptId } returns msgIn.taskAttemptId
    every { state.taskAttemptIndex } returns msgIn.taskAttemptIndex + 1
    every { logger.warn(any(), msgIn, state) } returns "warning!"
    // given
    val engine = TaskEngine()
    engine.taskDispatcher = taskDispatcher
    engine.workflowDispatcher = workflowDispatcher
    engine.stater = stater
    engine.logger = logger
    // when
    engine.handle(msg = msgIn)
    // then
    verify(exactly = 1) { logger.warn(any(), msgIn, state) }
    verify(exactly = 1) { stater.getState(msgIn.getStateId()) }
    confirmVerified(taskDispatcher)
    confirmVerified(workflowDispatcher)
    confirmVerified(stater)
    confirmVerified(logger)
}
