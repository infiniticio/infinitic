package com.zenaton.taskmanager.engine

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskmanager.data.TaskState
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.dispatcher.TaskDispatcher
import com.zenaton.taskmanager.logger.TaskLogger
import com.zenaton.taskmanager.messages.engine.CancelTask
import com.zenaton.taskmanager.messages.engine.DispatchTask
import com.zenaton.taskmanager.messages.engine.RetryTask
import com.zenaton.taskmanager.messages.engine.RetryTaskAttempt
import com.zenaton.taskmanager.messages.engine.TaskAttemptCompleted
import com.zenaton.taskmanager.messages.engine.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.engine.TaskAttemptFailed
import com.zenaton.taskmanager.messages.engine.TaskAttemptStarted
import com.zenaton.taskmanager.messages.engine.TaskCanceled
import com.zenaton.taskmanager.messages.engine.TaskCompleted
import com.zenaton.taskmanager.messages.engine.TaskDispatched
import com.zenaton.taskmanager.messages.engine.TaskEngineMessage
import com.zenaton.taskmanager.messages.interfaces.TaskAttemptMessage
import com.zenaton.taskmanager.messages.metrics.TaskStatusUpdated
import com.zenaton.taskmanager.messages.workers.RunTask
import com.zenaton.taskmanager.stater.TaskStaterInterface
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
fun taskCompleted(values: Map<String, Any?>? = null) = TestFactory.get(TaskCompleted::class, values)
fun taskDispatched(values: Map<String, Any?>? = null) = TestFactory.get(TaskDispatched::class, values)

class EngineResults {
    lateinit var taskDispatcher: TaskDispatcher
    lateinit var workflowDispatcher: WorkflowDispatcher
    lateinit var stater: TaskStaterInterface
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
    var taskCompleted: TaskCompleted? = null
    var taskDispatched: TaskDispatched? = null
    var taskCompletedInWorkflow: TaskCompletedInWorkflow? = null
    var taskStatusUpdated: TaskStatusUpdated? = null
}

fun engineHandle(stateIn: TaskState?, msgIn: TaskEngineMessage): EngineResults {
    // avoid deep updates of stateIn
    val state = stateIn?.copy()
    // mocking
    val taskDispatcher = mockk<TaskDispatcher>()
    val workflowDispatcher = mockk<WorkflowDispatcher>()
    val stater = mockk<TaskStaterInterface>()
    val logger = mockk<TaskLogger>()
    val stateSlot = slot<TaskState>()
    val taskAttemptCompletedSlot = slot<TaskAttemptCompleted>()
    val taskAttemptDispatchedSlot = slot<TaskAttemptDispatched>()
    val taskAttemptFailedSlot = slot<TaskAttemptFailed>()
    val taskAttemptStartedSlot = slot<TaskAttemptStarted>()
    val taskCanceledSlot = slot<TaskCanceled>()
    val taskCompletedSlot = slot<TaskCompleted>()
    val taskDispatchedSlot = slot<TaskDispatched>()
    val retryTaskAttemptSlot = slot<RetryTaskAttempt>()
    val retryTaskAttemptDelaySlot = slot<Float>()
    val runTaskSlot = slot<RunTask>()
    val taskCompletedInWorkflowSlot = slot<TaskCompletedInWorkflow>()
    val taskStatusUpdatedSlot = slot<TaskStatusUpdated>()
    every { logger.error(any(), msgIn, stateIn) } returns "error!"
    every { logger.warn(any(), msgIn, stateIn) } returns "warn!"
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
    every { taskDispatcher.dispatch(capture(taskCompletedSlot)) } just Runs
    every { taskDispatcher.dispatch(capture(taskDispatchedSlot)) } just Runs
    every { taskDispatcher.dispatch(capture(taskStatusUpdatedSlot)) } just Runs
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
    if (taskCompletedSlot.isCaptured) o.taskCompleted = taskCompletedSlot.captured
    if (taskDispatchedSlot.isCaptured) o.taskDispatched = taskDispatchedSlot.captured
    if (taskStatusUpdatedSlot.isCaptured) o.taskStatusUpdated = taskStatusUpdatedSlot.captured
    if (taskCompletedInWorkflowSlot.isCaptured) o.taskCompletedInWorkflow = taskCompletedInWorkflowSlot.captured

    return o
}

class TaskEngineTests : StringSpec({
    // Note: dispatchTask is voluntary excluded of this test
    include(shouldWarnIfNotState(cancelTask()))
    include(shouldWarnIfNotState(retryTask()))
    include(shouldWarnIfNotState(retryTaskAttempt()))
    include(shouldWarnIfNotState(taskAttemptCompleted()))
    include(shouldWarnIfNotState(taskAttemptDispatched()))
    include(shouldWarnIfNotState(taskAttemptFailed()))
    include(shouldWarnIfNotState(taskAttemptStarted()))
    include(shouldWarnIfNotState(taskCanceled()))
    include(shouldWarnIfNotState(taskCompleted()))
    include(shouldWarnIfNotState(taskDispatched()))

    include(shouldErrorIfStateAndMessageHaveInconsistentTaskId(cancelTask()))
    include(shouldErrorIfStateAndMessageHaveInconsistentTaskId(retryTask()))
    include(shouldErrorIfStateAndMessageHaveInconsistentTaskId(retryTaskAttempt()))
    include(shouldErrorIfStateAndMessageHaveInconsistentTaskId(taskAttemptCompleted()))
    include(shouldErrorIfStateAndMessageHaveInconsistentTaskId(taskAttemptDispatched()))
    include(shouldErrorIfStateAndMessageHaveInconsistentTaskId(taskAttemptFailed()))
    include(shouldErrorIfStateAndMessageHaveInconsistentTaskId(taskAttemptStarted()))
    include(shouldErrorIfStateAndMessageHaveInconsistentTaskId(taskCanceled()))
    include(shouldErrorIfStateAndMessageHaveInconsistentTaskId(taskCompleted()))
    include(shouldErrorIfStateAndMessageHaveInconsistentTaskId(taskDispatched()))

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
            o.logger.error(any(), msgIn, stateIn)
        }
        checkConfirmVerified(o)
    }

    "Cancel Task" {
        val stateIn = state(mapOf(
            "taskStatus" to TaskStatus.OK
        ))
        val msgIn = cancelTask(mapOf("taskId" to stateIn.taskId))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.stater.getState(msgIn.getStateId())
            o.stater.deleteState(msgIn.getStateId())
            o.taskDispatcher.dispatch(o.taskCanceled!!)
            o.taskDispatcher.dispatch(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.taskCanceled!!.taskId shouldBe msgIn.taskId
        o.taskStatusUpdated!!.oldStatus shouldBe stateIn.taskStatus
        o.taskStatusUpdated!!.newStatus shouldBe null
    }

    "Dispatch Task" {
        val msgIn = dispatchTask()
        val o = engineHandle(null, msgIn)
        verifyOrder {
            o.stater.getState(msgIn.getStateId())
            o.taskDispatcher.dispatch(o.runTask!!)
            o.taskDispatcher.dispatch(o.taskDispatched!!)
            o.taskDispatcher.dispatch(o.taskAttemptDispatched!!)
            o.stater.createState(msgIn.getStateId(), o.state!!)
            o.taskDispatcher.dispatch(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
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
        o.state!!.taskStatus shouldBe TaskStatus.OK
        o.taskStatusUpdated!!.oldStatus shouldBe null
        o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.OK
    }

    "Retry Task" {
        val stateIn = state(mapOf(
            "taskStatus" to TaskStatus.ERROR
        ))
        val msgIn = retryTask(mapOf("taskId" to stateIn.taskId))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.stater.getState(msgIn.getStateId())
            o.taskDispatcher.dispatch(o.runTask!!)
            o.taskDispatcher.dispatch(o.taskAttemptDispatched!!)
            o.stater.updateState(msgIn.getStateId(), o.state!!)
            o.taskDispatcher.dispatch(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
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
        o.state!!.taskStatus shouldBe TaskStatus.WARNING
        o.taskStatusUpdated!!.oldStatus shouldBe stateIn.taskStatus
        o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.WARNING
    }

    "Retry Task Attempt" {
        val stateIn = state(mapOf(
            "taskStatus" to TaskStatus.ERROR
        ))
        val msgIn = retryTaskAttempt(mapOf(
            "taskId" to stateIn.taskId,
            "taskAttemptId" to stateIn.taskAttemptId,
            "taskAttemptIndex" to stateIn.taskAttemptIndex
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldRetryTaskAttempt(msgIn, stateIn, o)
    }

    "Task Attempt Completed" {
        val stateIn = state(mapOf(
            "taskStatus" to TaskStatus.OK
        ))
        val msgIn = taskAttemptCompleted(mapOf(
            "taskId" to stateIn.taskId,
            "workflowId" to TestFactory.get(WorkflowId::class)
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.stater.getState(msgIn.getStateId())
            o.workflowDispatcher.dispatch(o.taskCompletedInWorkflow!!)
            o.taskDispatcher.dispatch(o.taskCompleted!!)
            o.stater.deleteState(msgIn.getStateId())
            o.taskDispatcher.dispatch(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.taskCompletedInWorkflow!!.taskId shouldBe stateIn.taskId
        o.taskCompletedInWorkflow!!.workflowId shouldBe stateIn.workflowId
        o.taskCompletedInWorkflow!!.taskOutput shouldBe msgIn.taskOutput
        o.taskStatusUpdated!!.oldStatus shouldBe stateIn.taskStatus
        o.taskStatusUpdated!!.newStatus shouldBe null
    }

    "Task Attempt Failed without retry" {
        val stateIn = state(mapOf(
            "taskStatus" to TaskStatus.OK
        ))
        val msgIn = taskAttemptFailed(mapOf(
            "taskId" to stateIn.taskId,
            "taskAttemptId" to stateIn.taskAttemptId,
            "taskAttemptIndex" to stateIn.taskAttemptIndex,
            "taskAttemptDelayBeforeRetry" to null
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.stater.getState(msgIn.getStateId())
            o.taskDispatcher.dispatch(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
    }

    "Task Attempt Failed with future retry" {
        val stateIn = state(mapOf(
            "taskStatus" to TaskStatus.OK
        ))
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
            o.stater.updateState(msgIn.getStateId(), o.state!!)
            o.taskDispatcher.dispatch(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.retryTaskAttempt!!.taskId shouldBe stateIn.taskId
        o.retryTaskAttempt!!.taskAttemptId shouldBe stateIn.taskAttemptId
        o.retryTaskAttempt!!.taskAttemptIndex shouldBe stateIn.taskAttemptIndex
        o.retryTaskAttemptDelay!! shouldBe msgIn.taskAttemptDelayBeforeRetry
        o.taskStatusUpdated!!.taskId shouldBe stateIn.taskId
        o.taskStatusUpdated!!.taskName shouldBe stateIn.taskName
        o.taskStatusUpdated!!.oldStatus shouldBe stateIn.taskStatus
        o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.WARNING
    }

    "Task Attempt Failed with immediate retry" {
        val stateIn = state(mapOf(
            "taskStatus" to TaskStatus.OK
        ))
        val msgIn = taskAttemptFailed(mapOf(
            "taskId" to stateIn.taskId,
            "taskAttemptId" to stateIn.taskAttemptId,
            "taskAttemptIndex" to stateIn.taskAttemptIndex,
            "taskAttemptDelayBeforeRetry" to 0F
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldRetryTaskAttempt(msgIn, stateIn, o)
    }

    "Task Attempt Failed with immediate retry (negative delay)" {
        val stateIn = state(mapOf(
            "taskStatus" to TaskStatus.OK
        ))
        val msgIn = taskAttemptFailed(mapOf(
            "taskId" to stateIn.taskId,
            "taskAttemptId" to stateIn.taskAttemptId,
            "taskAttemptIndex" to stateIn.taskAttemptIndex,
            "taskAttemptDelayBeforeRetry" to -42F
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldRetryTaskAttempt(msgIn, stateIn, o)
    }

    "Task Attempt Started" {
        val stateIn = state(mapOf(
            "taskStatus" to TaskStatus.OK
        ))
        val msgIn = taskAttemptStarted(mapOf(
            "taskId" to stateIn.taskId,
            "taskAttemptId" to stateIn.taskAttemptId,
            "taskAttemptIndex" to stateIn.taskAttemptIndex
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(msgIn, o)
    }

    "Task Canceled" {
        val stateIn = state()
        val msgIn = taskCanceled(mapOf(
            "taskId" to stateIn.taskId
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(msgIn, o)
    }

    "Task Completed" {
        val stateIn = state()
        val msgIn = taskCompleted(mapOf(
            "taskId" to stateIn.taskId
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(msgIn, o)
    }

    "Task Dispatched" {
        val stateIn = state()
        val msgIn = taskDispatched(mapOf(
            "taskId" to stateIn.taskId
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(msgIn, o)
    }
})

private fun shouldWarnIfNotState(msgIn: TaskEngineMessage) = stringSpec {
    val o = engineHandle(null, msgIn)
    checkShouldWarnAndDoNothingMore(null, msgIn, o)
}

private fun shouldErrorIfStateAndMessageHaveInconsistentTaskId(msgIn: TaskEngineMessage) = stringSpec {
    val stateIn = state()
    val o = engineHandle(stateIn, msgIn)
    checkShouldErrorAndDoNothingMore(stateIn, msgIn, o)
}

private fun shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(msgIn: TaskAttemptMessage) = stringSpec {
    val stateIn = state(mapOf("taskId" to msgIn.taskId))
    val o = engineHandle(stateIn, msgIn as TaskEngineMessage)
    checkShouldWarnAndDoNothingMore(stateIn, msgIn as TaskEngineMessage, o)
}

private fun shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(msgIn: TaskAttemptMessage) = stringSpec {
    val stateIn = state(mapOf(
        "taskId" to msgIn.taskId,
        "taskAttemptId" to msgIn.taskAttemptId
    ))
    val o = engineHandle(stateIn, msgIn as TaskEngineMessage)
    checkShouldWarnAndDoNothingMore(stateIn, msgIn as TaskEngineMessage, o)
}

private fun checkShouldDoNothing(msgIn: TaskEngineMessage, o: EngineResults) {
    verifyOrder {
        o.stater.getState(msgIn.getStateId())
    }
    checkConfirmVerified(o)
}

private fun checkShouldRetryTaskAttempt(msgIn: TaskEngineMessage, stateIn: TaskState, o: EngineResults) {
    verifyOrder {
        o.stater.getState(msgIn.getStateId())
        o.taskDispatcher.dispatch(o.runTask!!)
        o.taskDispatcher.dispatch(o.taskAttemptDispatched!!)
        o.stater.updateState(msgIn.getStateId(), o.state!!)
        o.taskDispatcher.dispatch(o.taskStatusUpdated!!)
    }
    checkConfirmVerified(o)
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
    o.state!!.taskStatus shouldBe TaskStatus.WARNING
    o.taskStatusUpdated!!.oldStatus shouldBe stateIn.taskStatus
    o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.WARNING
}

private fun checkShouldWarnAndDoNothingMore(stateIn: TaskState?, msgIn: TaskEngineMessage, o: EngineResults) {
    verifyOrder {
        o.stater.getState(msgIn.getStateId())
        o.logger.warn(any(), msgIn, stateIn)
    }
    checkConfirmVerified(o)
}

private fun checkShouldErrorAndDoNothingMore(stateIn: TaskState?, msgIn: TaskEngineMessage, o: EngineResults) {
    verifyOrder {
        o.stater.getState(msgIn.getStateId())
        o.logger.error(any(), msgIn, stateIn)
    }
    checkConfirmVerified(o)
}

private fun checkConfirmVerified(o: EngineResults) {
    confirmVerified(o.taskDispatcher)
    confirmVerified(o.workflowDispatcher)
    confirmVerified(o.stater)
    confirmVerified(o.logger)
}
