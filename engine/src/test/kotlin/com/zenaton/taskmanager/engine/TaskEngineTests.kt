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

fun state(values: Map<String, Any?>? = null) = TestFactory.get(TaskState::class, values)

fun cancelTask(values: Map<String, Any?>? = null) = TestFactory.get(CancelTask::class, values)
fun dispatchTask(values: Map<String, Any?>? = null) = TestFactory.get(DispatchTask::class, values)
fun retryTask(values: Map<String, Any?>? = null) = TestFactory.get(RetryTask::class, values)
fun retryTaskAttempt(values: Map<String, Any?>? = null) = TestFactory.get(RetryTaskAttempt::class, values)
fun runTask(values: Map<String, Any?>? = null) = TestFactory.get(RunTask::class, values)

fun taskAttemptCompleted(values: Map<String, Any?>? = null) = TestFactory.get(TaskAttemptCompleted::class, values)
fun taskAttemptDispatched(values: Map<String, Any?>? = null) = TestFactory.get(TaskAttemptDispatched::class, values)
fun taskAttemptFailed(values: Map<String, Any?>? = null) = TestFactory.get(TaskAttemptFailed::class, values)
fun taskAttemptStarted(values: Map<String, Any?>? = null) = TestFactory.get(TaskAttemptStarted::class, values)
fun taskCanceled(values: Map<String, Any?>? = null) = TestFactory.get(TaskCanceled::class, values)

class EngineResults {
    lateinit var taskDispatcher: TaskDispatcher
    lateinit var workflowDispatcher: WorkflowDispatcher
    lateinit var stater: TaskStater
    lateinit var logger: TaskLogger
    var state: TaskState? = null
    var runTask: RunTask? = null
    var retryTaskAttempt: RetryTaskAttempt? = null
    var retryTaskAttemptDelay: Float? = null
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
    val retryTaskAttemptSlot = slot<RetryTaskAttempt>()
    val retryTaskAttemptDelaySlot = slot<Float>()
    val runTaskSlot = slot<RunTask>()
    val taskCompletedInWorkflowSlot = slot<TaskCompletedInWorkflow>()
    every { logger.error(any(), msgIn) } returns "error!"
    every { logger.warn(any(), msgIn) } returns "warn!"
    every { stater.getState(msgIn.getStateId()) } returns state
    every { stater.createState(any(), capture(stateSlot)) } just Runs
    every { stater.updateState(any(), capture(stateSlot)) } just Runs
    every { stater.deleteState(any()) } just Runs
    every { taskDispatcher.dispatch(capture(runTaskSlot)) } just Runs
    every { taskDispatcher.dispatch(capture(retryTaskAttemptSlot), capture(retryTaskAttemptDelaySlot)) } just Runs
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
    if (retryTaskAttemptSlot.isCaptured) o.retryTaskAttempt = retryTaskAttemptSlot.captured
    if (retryTaskAttemptDelaySlot.isCaptured) o.retryTaskAttemptDelay = retryTaskAttemptDelaySlot.captured
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

    // Note: taskAttemptCompleted is voluntary excluded of this test
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(retryTaskAttempt()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(taskAttemptDispatched()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(taskAttemptFailed()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(taskAttemptStarted()))

    // Note: taskAttemptCompleted is voluntary excluded of this test
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(retryTaskAttempt()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(taskAttemptDispatched()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(taskAttemptFailed()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(taskAttemptStarted()))

    "Should error if task dispatched with existing state" {
        val stateIn = state()
        val msgIn = dispatchTask(mapOf("taskId" to stateIn.taskId))
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
        val stateIn = state()
        val msgIn = cancelTask(mapOf("taskId" to stateIn.taskId))
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
        val stateIn = state()
        val msgIn = retryTask(mapOf("taskId" to stateIn.taskId))
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
        val stateIn = state()
        val msgIn = retryTaskAttempt(mapOf(
            "taskId" to stateIn.taskId,
            "taskAttemptId" to stateIn.taskAttemptId,
            "taskAttemptIndex" to stateIn.taskAttemptIndex
        ))
        val o = engineHandle(stateIn, msgIn)
        shouldRetryTaskAttempt(msgIn, stateIn, o)
    }

    "Task Attempt Completed" {
        val stateIn = state()
        val msgIn = taskAttemptCompleted(mapOf(
            "taskId" to stateIn.taskId,
            "workflowId" to TestFactory.get(WorkflowId::class)
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.stater.getState(msgIn.getStateId())
            o.workflowDispatcher.dispatch(o.taskCompletedInWorkflow!!)
            o.stater.deleteState(msgIn.getStateId())
        }
        confirmVerified(o.taskDispatcher)
        confirmVerified(o.workflowDispatcher)
        confirmVerified(o.stater)
        confirmVerified(o.logger)
        o.taskCompletedInWorkflow!!.taskId shouldBe stateIn.taskId
        o.taskCompletedInWorkflow!!.workflowId shouldBe stateIn.workflowId
        o.taskCompletedInWorkflow!!.taskOutput shouldBe msgIn.taskOutput
    }

    "Task Attempt Failed without retry" {
        val stateIn = state()
        val msgIn = taskAttemptFailed(mapOf(
            "taskId" to stateIn.taskId,
            "taskAttemptId" to stateIn.taskAttemptId,
            "taskAttemptIndex" to stateIn.taskAttemptIndex,
            "taskAttemptDelayBeforeRetry" to null
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.stater.getState(msgIn.getStateId())
        }
        confirmVerified(o.taskDispatcher)
        confirmVerified(o.workflowDispatcher)
        confirmVerified(o.stater)
        confirmVerified(o.logger)
    }

    "Task Attempt Failed with future retry" {
        val stateIn = state()
        val msgIn = taskAttemptFailed(mapOf(
            "taskId" to stateIn.taskId,
            "taskAttemptId" to stateIn.taskAttemptId,
            "taskAttemptIndex" to stateIn.taskAttemptIndex,
            "taskAttemptDelayBeforeRetry" to 42F
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.stater.getState(msgIn.getStateId())
            o.taskDispatcher.dispatch(o.retryTaskAttempt!!, o.retryTaskAttemptDelay!!)
        }
        confirmVerified(o.taskDispatcher)
        confirmVerified(o.workflowDispatcher)
        confirmVerified(o.stater)
        confirmVerified(o.logger)
    }

    "Task Attempt Failed with immediate retry" {
        val stateIn = state()
        val msgIn = taskAttemptFailed(mapOf(
            "taskId" to stateIn.taskId,
            "taskAttemptId" to stateIn.taskAttemptId,
            "taskAttemptIndex" to stateIn.taskAttemptIndex,
            "taskAttemptDelayBeforeRetry" to 0F
        ))
        val o = engineHandle(stateIn, msgIn)
        shouldRetryTaskAttempt(msgIn, stateIn, o)
    }

    "Task Attempt Failed with immediate retry (negative delay)" {
        val stateIn = state()
        val msgIn = taskAttemptFailed(mapOf(
            "taskId" to stateIn.taskId,
            "taskAttemptId" to stateIn.taskAttemptId,
            "taskAttemptIndex" to stateIn.taskAttemptIndex,
            "taskAttemptDelayBeforeRetry" to -42F
        ))
        val o = engineHandle(stateIn, msgIn)
        shouldRetryTaskAttempt(msgIn, stateIn, o)
    }

    "Task Attempt Started" {
        val stateIn = state()
        val msgIn = taskAttemptStarted(mapOf(
            "taskId" to stateIn.taskId,
            "taskAttemptId" to stateIn.taskAttemptId,
            "taskAttemptIndex" to stateIn.taskAttemptIndex
        ))
        val o = engineHandle(stateIn, msgIn)
        shouldDoNothing(msgIn, o)
    }

    "Task Canceled" {
        val stateIn = state()
        val msgIn = taskCanceled(mapOf(
            "taskId" to stateIn.taskId
        ))
        val o = engineHandle(stateIn, msgIn)
        shouldDoNothing(msgIn, o)
    }
})

fun shouldDoNothing(msgIn: TaskMessageInterface, o:EngineResults) {
    verifyOrder {
        o.stater.getState(msgIn.getStateId())
    }
    confirmVerified(o.taskDispatcher)
    confirmVerified(o.workflowDispatcher)
    confirmVerified(o.stater)
    confirmVerified(o.logger)
}

fun shouldRetryTaskAttempt(msgIn: TaskMessageInterface, stateIn: TaskState, o:EngineResults) {
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
