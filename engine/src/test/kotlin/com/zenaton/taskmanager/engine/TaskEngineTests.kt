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
import com.zenaton.workflowengine.data.WorkflowId
import com.zenaton.workflowengine.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import com.zenaton.workflowengine.topics.workflows.messages.TaskCompleted as TaskCompletedInWorkflow
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

class EngineResults {
    lateinit var taskDispatcher: TaskDispatcher
    lateinit var workflowDispatcher: WorkflowDispatcher
    lateinit var stater: TaskStater
    lateinit var logger: TaskLogger
    var state: TaskState? = null
    var runTask: RunTask? = null
    var taskAttemptCompleted: TaskAttemptCompleted? = null
    var taskAttemptDispatched: TaskAttemptDispatched? = null
    var taskAttemptFailed: TaskAttemptFailed? = null
    var taskAttemptStarted: TaskAttemptStarted? = null
    var taskCanceled: TaskCanceled? = null
    var taskCompletedInWorkflow: TaskCompletedInWorkflow? = null
}

fun engineHandle(stateIn: TaskState?, msgIn: TaskMessageInterface): EngineResults {
    // avoid deep updates of stateIn
    val state = stateIn?.copy()
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
    val taskCompletedInWorkflowSlot = slot<TaskCompletedInWorkflow>()
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
    every { workflowDispatcher.dispatch(capture(taskCompletedInWorkflowSlot)) } just Runs
    // given
    val engine = TaskEngine()
    engine.taskDispatcher = taskDispatcher
    engine.workflowDispatcher = workflowDispatcher
    engine.stater = stater
    engine.logger = logger
    // when
    engine.handle(msg = msgIn)
    // then
    val o = EngineResults()
    o.taskDispatcher = taskDispatcher
    o.workflowDispatcher = workflowDispatcher
    o.stater = stater
    o.logger = logger
    if (stateSlot.isCaptured) o.state = stateSlot.captured
    if (runTaskSlot.isCaptured) o.runTask = runTaskSlot.captured
    if (taskAttemptCompletedSlot.isCaptured) o.taskAttemptCompleted = taskAttemptCompletedSlot.captured
    if (taskAttemptDispatchedSlot.isCaptured) o.taskAttemptDispatched = taskAttemptDispatchedSlot.captured
    if (taskAttemptFailedSlot.isCaptured) o.taskAttemptFailed = taskAttemptFailedSlot.captured
    if (taskAttemptStartedSlot.isCaptured) o.taskAttemptStarted = taskAttemptStartedSlot.captured
    if (taskCanceledSlot.isCaptured) o.taskCanceled = taskCanceledSlot.captured
    if (taskCompletedInWorkflowSlot.isCaptured) o.taskCompletedInWorkflow = taskCompletedInWorkflowSlot.captured

    return o
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
        val stateIn = TestFactory.get(TaskState::class, mapOf("taskId" to msgIn.taskId))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.stater.getState(msgIn.getStateId())
            o.logger.error(any(), msgIn)
        }
        confirmVerified(o.taskDispatcher)
        confirmVerified(o.workflowDispatcher)
        confirmVerified(o.stater)
        confirmVerified(o.logger)
    }

    "Cancel Task" {
        val msgIn = cancelTask()
        val stateIn = TestFactory.get(TaskState::class, mapOf("taskId" to msgIn.taskId))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.stater.getState(msgIn.getStateId())
            o.stater.deleteState(msgIn.getStateId())
            o.taskDispatcher.dispatch(o.taskCanceled!!)
        }
        confirmVerified(o.taskDispatcher)
        confirmVerified(o.workflowDispatcher)
        confirmVerified(o.stater)
        confirmVerified(o.logger)
        o.taskCanceled!!.taskId shouldBe msgIn.taskId
    }

    "Dispatch Task" {
        val msgIn = dispatchTask()
        val o = engineHandle(null, msgIn)
        verifyOrder {
            o.stater.getState(msgIn.getStateId())
            o.taskDispatcher.dispatch(o.runTask!!)
            o.taskDispatcher.dispatch(o.taskAttemptDispatched!!)
            o.stater.createState(msgIn.getStateId(), o.state!!)
        }
        confirmVerified(o.taskDispatcher)
        confirmVerified(o.workflowDispatcher)
        confirmVerified(o.stater)
        confirmVerified(o.logger)
        o.runTask!!.taskId shouldBe msgIn.taskId
        o.runTask!!.taskName shouldBe msgIn.taskName
        o.runTask!!.taskData shouldBe msgIn.taskData
        o.runTask!!.taskAttemptIndex shouldBe 0
        o.runTask!!.taskAttemptId shouldBe o.taskAttemptDispatched!!.taskAttemptId
        o.runTask!!.taskAttemptIndex shouldBe o.taskAttemptDispatched!!.taskAttemptIndex
        o.state!!.taskId shouldBe msgIn.taskId
        o.state!!.taskName shouldBe msgIn.taskName
        o.state!!.taskData shouldBe msgIn.taskData
        o.state!!.taskAttemptId shouldBe o.runTask!!.taskAttemptId
        o.state!!.taskAttemptIndex shouldBe 0
        o.state!!.workflowId shouldBe msgIn.workflowId
    }

    "Retry Task" {
        val msgIn = retryTask()
        val stateIn = TestFactory.get(TaskState::class, mapOf("taskId" to msgIn.taskId))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.stater.getState(msgIn.getStateId())
            o.taskDispatcher.dispatch(o.runTask!!)
            o.taskDispatcher.dispatch(o.taskAttemptDispatched!!)
            o.stater.updateState(msgIn.getStateId(), o.state!!)
        }
        confirmVerified(o.taskDispatcher)
        confirmVerified(o.workflowDispatcher)
        confirmVerified(o.stater)
        confirmVerified(o.logger)
        o.runTask!!.taskId shouldBe stateIn.taskId
        o.runTask!!.taskAttemptId shouldNotBe stateIn.taskAttemptId
        o.runTask!!.taskAttemptIndex shouldBe 0
        o.runTask!!.taskName shouldBe stateIn.taskName
        o.runTask!!.taskData shouldBe stateIn.taskData
        o.taskAttemptDispatched!!.taskId shouldBe stateIn.taskId
        o.taskAttemptDispatched!!.taskAttemptId shouldBe o.runTask!!.taskAttemptId
        o.taskAttemptDispatched!!.taskAttemptIndex shouldBe 0
        o.state!!.taskId shouldBe stateIn.taskId
        o.state!!.taskName shouldBe stateIn.taskName
        o.state!!.taskData shouldBe stateIn.taskData
        o.state!!.taskAttemptId shouldBe o.runTask!!.taskAttemptId
        o.state!!.taskAttemptIndex shouldBe o.runTask!!.taskAttemptIndex
    }

    "Retry Task Attempt" {
        val msgIn = retryTaskAttempt()
        val stateIn = TestFactory.get(TaskState::class, mapOf(
            "taskId" to msgIn.taskId,
            "taskAttemptId" to msgIn.taskAttemptId,
            "taskAttemptIndex" to msgIn.taskAttemptIndex
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.stater.getState(msgIn.getStateId())
            o.taskDispatcher.dispatch(o.runTask!!)
            o.taskDispatcher.dispatch(o.taskAttemptDispatched!!)
            o.stater.updateState(msgIn.getStateId(), o.state!!)
        }
        confirmVerified(o.taskDispatcher)
        confirmVerified(o.workflowDispatcher)
        confirmVerified(o.stater)
        confirmVerified(o.logger)
        o.runTask!!.taskId shouldBe stateIn.taskId
        o.runTask!!.taskAttemptId shouldBe stateIn.taskAttemptId
        o.runTask!!.taskAttemptIndex shouldBe stateIn.taskAttemptIndex + 1
        o.runTask!!.taskName shouldBe stateIn.taskName
        o.runTask!!.taskData shouldBe stateIn.taskData
        o.taskAttemptDispatched!!.taskId shouldBe stateIn.taskId
        o.taskAttemptDispatched!!.taskAttemptId shouldBe o.runTask!!.taskAttemptId
        o.taskAttemptDispatched!!.taskAttemptIndex shouldBe o.runTask!!.taskAttemptIndex
        o.state!!.taskId shouldBe stateIn.taskId
        o.state!!.taskName shouldBe stateIn.taskName
        o.state!!.taskData shouldBe stateIn.taskData
        o.state!!.taskAttemptId shouldBe o.runTask!!.taskAttemptId
        o.state!!.taskAttemptIndex shouldBe o.runTask!!.taskAttemptIndex
    }
//
//    "Task Attempt Completed" {
//        val msgIn = taskAttemptCompleted()
//        val stateIn = TestFactory.get(TaskState::class, mapOf(
//            "taskId" to msgIn.taskId,
//            "workflowId" to TestFactory.get(WorkflowId::class)
//        ))
//        val o = engineHandle(stateIn, msgIn)
//        verifyOrder {
//            o.stater.getState(msgIn.getStateId())
//            o.workflowDispatcher.dispatch(o.taskCompletedInWorkflow!!)
//            o.stater.deleteState(msgIn.getStateId())
//        }
//        confirmVerified(o.taskDispatcher)
//        confirmVerified(o.workflowDispatcher)
//        confirmVerified(o.stater)
//        confirmVerified(o.logger)
//        o.taskCompletedInWorkflow!!.taskId shouldBe stateIn.taskId
//        o.taskCompletedInWorkflow!!.workflowId shouldBe stateIn.workflowId
//        o.taskCompletedInWorkflow!!.taskOutput shouldBe msgIn.taskOutput
//    }
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
