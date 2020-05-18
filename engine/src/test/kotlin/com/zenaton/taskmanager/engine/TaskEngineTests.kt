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

fun test(state: TaskState?, msgIn: TaskMessageInterface): Map<String, Any?> {
    // mocking
    val taskDispatcher = mockk<TaskDispatcher>()
    val workflowDispatcher = mockk<WorkflowDispatcher>()
    val stater = mockk<TaskStater>()
    val logger = mockk<TaskLogger>()
    val stateSlot = slot<TaskState>()
    val taskAttemptCompletedSlot = slot<TaskAttemptCompleted>()
    val taskAttemptDispatchedSlot = slot<TaskAttemptDispatched>()
    val taskAttemptFailedSlot = slot<TaskAttemptFailed>()
    val taskAttemptStartedSlot = slot<TaskAttemptStarted>()
    val taskCanceledSlot = slot<TaskCanceled>()
    val runTaskSlot = slot<RunTask>()
    every { logger.error(any(), msgIn) } returns "error!"
    every { logger.warn(any(), msgIn) } returns "warn!"
    every { stater.getState(msgIn.getStateId()) } returns state
    every { stater.createState(any(), capture(stateSlot)) } just Runs
    every { stater.updateState(any(), capture(stateSlot)) } just Runs
    every { stater.deleteState(any()) } just Runs
    every { taskDispatcher.dispatch(capture(runTaskSlot)) } just Runs
    every { taskDispatcher.dispatch(capture(taskAttemptCompletedSlot)) } just Runs
    every { taskDispatcher.dispatch(capture(taskAttemptDispatchedSlot)) } just Runs
    every { taskDispatcher.dispatch(capture(taskAttemptFailedSlot)) } just Runs
    every { taskDispatcher.dispatch(capture(taskAttemptStartedSlot)) } just Runs
    every { taskDispatcher.dispatch(capture(taskCanceledSlot)) } just Runs
    // given
    val engine = TaskEngine()
    engine.taskDispatcher = taskDispatcher
    engine.workflowDispatcher = workflowDispatcher
    engine.stater = stater
    engine.logger = logger
    // when
    engine.handle(msg = msgIn)
    // then
    return mapOf(
        "taskDispatcher" to taskDispatcher,
        "workflowDispatcher" to workflowDispatcher,
        "stater" to stater,
        "logger" to logger,
        "state" to if (stateSlot.isCaptured) stateSlot.captured else null,
        "runTask" to if (runTaskSlot.isCaptured) runTaskSlot.captured else null,
        "taskAttemptCompleted" to if (taskAttemptCompletedSlot.isCaptured) taskAttemptCompletedSlot.captured else null,
        "taskAttemptDispatched" to if (taskAttemptDispatchedSlot.isCaptured) taskAttemptDispatchedSlot.captured else null,
        "taskAttemptFailed" to if (taskAttemptFailedSlot.isCaptured) taskAttemptFailedSlot.captured else null,
        "taskAttemptStarted" to if (taskAttemptStartedSlot.isCaptured) taskAttemptStartedSlot.captured else null,
        "taskCanceled" to if (taskCanceledSlot.isCaptured) taskCanceledSlot.captured else null
    )
}

inline fun <reified M : Any> getValue(results: Map<String, Any?>): M {
    return results.values.first { it is M } as M
}

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
        val stateIn = mockk<TaskState>()
        every { stateIn.taskId } returns msgIn.taskId
        val results = test(stateIn, msgIn)
        val taskDispatcher = getValue<TaskDispatcher>(results)
        val workflowDispatcher = getValue<WorkflowDispatcher>(results)
        val stater = getValue<TaskStater>(results)
        val logger = getValue<TaskLogger>(results)
        verifyOrder {
            stater.getState(msgIn.getStateId())
            logger.error(any(), msgIn)
        }
        confirmVerified(taskDispatcher)
        confirmVerified(workflowDispatcher)
        confirmVerified(stater)
        confirmVerified(logger)
    }

    "Cancel Task" {
        val msgIn = cancelTask()
        val stateIn = mockk<TaskState>()
        every { stateIn.taskId } returns msgIn.taskId
        val results = test(stateIn, msgIn)
        val taskDispatcher = getValue<TaskDispatcher>(results)
        val workflowDispatcher = getValue<WorkflowDispatcher>(results)
        val stater = getValue<TaskStater>(results)
        val logger = getValue<TaskLogger>(results)
        val taskCanceled = getValue<TaskCanceled>(results)
        verifyOrder {
            stater.getState(msgIn.getStateId())
            stater.deleteState(msgIn.getStateId())
            taskDispatcher.dispatch(taskCanceled)
        }
        confirmVerified(taskDispatcher)
        confirmVerified(workflowDispatcher)
        confirmVerified(stater)
        confirmVerified(logger)
        // then
        taskCanceled.taskId shouldBe msgIn.taskId
    }

    "Dispatch Task" {
        val msgIn = dispatchTask()
        val results = test(null, msgIn)
        val taskDispatcher = getValue<TaskDispatcher>(results)
        val workflowDispatcher = getValue<WorkflowDispatcher>(results)
        val stater = getValue<TaskStater>(results)
        val logger = getValue<TaskLogger>(results)
        val state = getValue<TaskState>(results)
        val runTask = getValue<RunTask>(results)
        val taskAttemptDispatched = getValue<TaskAttemptDispatched>(results)
        verifyOrder {
            stater.getState(msgIn.getStateId())
            taskDispatcher.dispatch(runTask)
            taskDispatcher.dispatch(taskAttemptDispatched)
            stater.createState(msgIn.getStateId(), state)
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
