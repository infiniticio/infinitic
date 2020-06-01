package com.zenaton.taskManager.engine

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskManager.data.TaskStatus
import com.zenaton.taskManager.dispatcher.Dispatcher
import com.zenaton.taskManager.logger.Logger
import com.zenaton.taskManager.messages.TaskAttemptMessage
import com.zenaton.taskManager.monitoring.perName.TaskStatusUpdated
import com.zenaton.taskManager.workers.RunTask
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

fun state(values: Map<String, Any?>? = null) = TestFactory.get(EngineState::class, values)

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
    lateinit var taskDispatcher: Dispatcher
    lateinit var workflowDispatcher: WorkflowDispatcher
    lateinit var storage: EngineStorage
    lateinit var logger: Logger
    var state: EngineState? = null
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

fun engineHandle(stateIn: EngineState?, msgIn: EngineMessage): EngineResults {
    // avoid deep updates of stateIn
    val state = stateIn?.copy()
    // mocking
    val taskDispatcher = mockk<Dispatcher>()
    val workflowDispatcher = mockk<WorkflowDispatcher>()
    val stateStorage = mockk<EngineStorage>()
    val logger = mockk<Logger>()
    val stateSlot = slot<EngineState>()
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
    every { stateStorage.getState(msgIn.taskId) } returns state
    every { stateStorage.updateState(any(), capture(stateSlot), any()) } just Runs
    every { stateStorage.deleteState(any()) } just Runs
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
    val engine = Engine()
    engine.taskDispatcher = taskDispatcher
    engine.storage = stateStorage
    engine.workflowDispatcher = workflowDispatcher
    engine.logger = logger
    // when
    engine.handle(msg = msgIn)
    // then
    val o = EngineResults()
    o.taskDispatcher = taskDispatcher
    o.workflowDispatcher = workflowDispatcher
    o.storage = stateStorage
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

class EngineTests : StringSpec({
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
            o.storage.getState(msgIn.taskId)
            o.logger.error(any(), msgIn, stateIn)
        }
        checkConfirmVerified(o)
    }

    "Cancel Task" {
        val stateIn = state(mapOf(
            "taskStatus" to TaskStatus.RUNNING_OK
        ))
        val msgIn = cancelTask(mapOf("taskId" to stateIn.taskId))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.taskId)
            o.taskDispatcher.dispatch(o.taskCanceled!!)
            o.storage.deleteState(msgIn.taskId)
            o.taskDispatcher.dispatch(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.taskCanceled!!.taskId shouldBe msgIn.taskId
        o.taskStatusUpdated!!.oldStatus shouldBe stateIn.taskStatus
        o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.TERMINATED_CANCELED
    }

    "Dispatch Task" {
        val msgIn = dispatchTask()
        val o = engineHandle(null, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.taskId)
            o.taskDispatcher.dispatch(o.runTask!!)
            o.taskDispatcher.dispatch(o.taskDispatched!!)
            o.taskDispatcher.dispatch(o.taskAttemptDispatched!!)
            o.storage.updateState(msgIn.taskId, o.state!!, null)
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
        o.state!!.taskStatus shouldBe TaskStatus.RUNNING_OK
        o.taskStatusUpdated!!.oldStatus shouldBe null
        o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.RUNNING_OK
    }

    "Retry Task" {
        val stateIn = state(mapOf(
            "taskStatus" to TaskStatus.RUNNING_ERROR
        ))
        val msgIn = retryTask(mapOf("taskId" to stateIn.taskId))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.taskId)
            o.taskDispatcher.dispatch(o.runTask!!)
            o.taskDispatcher.dispatch(o.taskAttemptDispatched!!)
            o.storage.updateState(msgIn.taskId, o.state!!, stateIn)
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
        o.state!!.taskStatus shouldBe TaskStatus.RUNNING_WARNING
        o.taskStatusUpdated!!.oldStatus shouldBe stateIn.taskStatus
        o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.RUNNING_WARNING
    }

    "Retry Task Attempt" {
        val stateIn = state(mapOf(
            "taskStatus" to TaskStatus.RUNNING_ERROR
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
            "taskStatus" to TaskStatus.RUNNING_OK
        ))
        val msgIn = taskAttemptCompleted(mapOf(
            "taskId" to stateIn.taskId,
            "workflowId" to TestFactory.get(WorkflowId::class)
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.taskId)
            o.workflowDispatcher.dispatch(o.taskCompletedInWorkflow!!)
            o.taskDispatcher.dispatch(o.taskCompleted!!)
            o.storage.deleteState(msgIn.taskId)
            o.taskDispatcher.dispatch(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.taskCompletedInWorkflow!!.taskId shouldBe stateIn.taskId
        o.taskCompletedInWorkflow!!.workflowId shouldBe stateIn.workflowId
        o.taskCompletedInWorkflow!!.taskOutput shouldBe msgIn.taskOutput
        o.taskStatusUpdated!!.oldStatus shouldBe stateIn.taskStatus
        o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.TERMINATED_COMPLETED
    }

    "Task Attempt Failed without retry" {
        val stateIn = state(mapOf(
            "taskStatus" to TaskStatus.RUNNING_OK
        ))
        val msgIn = taskAttemptFailed(mapOf(
            "taskId" to stateIn.taskId,
            "taskAttemptId" to stateIn.taskAttemptId,
            "taskAttemptIndex" to stateIn.taskAttemptIndex,
            "taskAttemptDelayBeforeRetry" to null
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.taskId)
            o.storage.updateState(msgIn.taskId, o.state!!, stateIn)
            o.taskDispatcher.dispatch(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
    }

    "Task Attempt Failed with future retry" {
        val stateIn = state(mapOf(
            "taskStatus" to TaskStatus.RUNNING_OK
        ))
        val msgIn = taskAttemptFailed(mapOf(
            "taskId" to stateIn.taskId,
            "taskAttemptId" to stateIn.taskAttemptId,
            "taskAttemptIndex" to stateIn.taskAttemptIndex,
            "taskAttemptDelayBeforeRetry" to 42F
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.taskId)
            o.taskDispatcher.dispatch(o.retryTaskAttempt!!, o.retryTaskAttemptDelay!!)
            o.storage.updateState(msgIn.taskId, o.state!!, stateIn)
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
        o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.RUNNING_WARNING
    }

    "Task Attempt Failed with immediate retry" {
        val stateIn = state(mapOf(
            "taskStatus" to TaskStatus.RUNNING_OK
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
            "taskStatus" to TaskStatus.RUNNING_OK
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
            "taskStatus" to TaskStatus.RUNNING_OK
        ))
        val msgIn = taskAttemptStarted(mapOf(
            "taskId" to stateIn.taskId,
            "taskAttemptId" to stateIn.taskAttemptId,
            "taskAttemptIndex" to stateIn.taskAttemptIndex
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(msgIn, o)
    }

    "Task attempt started on a OK task should do nothing" {
        val stateIn = state(mapOf(
            "taskStatus" to TaskStatus.RUNNING_OK
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
        verifyOrder {
            o.storage.getState(msgIn.taskId)
        }
        checkConfirmVerified(o)
    }

    "Task Completed" {
        val stateIn = state()
        val msgIn = taskCompleted(mapOf(
            "taskId" to stateIn.taskId
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.taskId)
        }
        checkConfirmVerified(o)
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

private fun shouldWarnIfNotState(msgIn: EngineMessage) = stringSpec {
    val o = engineHandle(null, msgIn)
    checkShouldWarnAndDoNothingMore(null, msgIn, o)
}

private fun shouldErrorIfStateAndMessageHaveInconsistentTaskId(msgIn: EngineMessage) = stringSpec {
    val stateIn = state()
    val o = engineHandle(stateIn, msgIn)
    checkShouldErrorAndDoNothingMore(stateIn, msgIn, o)
}

private fun shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(msgIn: TaskAttemptMessage) = stringSpec {
    val stateIn = state(mapOf("taskId" to msgIn.taskId))
    val o = engineHandle(stateIn, msgIn as EngineMessage)
    checkShouldWarnAndDoNothingMore(stateIn, msgIn as EngineMessage, o)
}

private fun shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(msgIn: TaskAttemptMessage) = stringSpec {
    val stateIn = state(mapOf(
        "taskId" to msgIn.taskId,
        "taskAttemptId" to msgIn.taskAttemptId
    ))
    val o = engineHandle(stateIn, msgIn as EngineMessage)
    checkShouldWarnAndDoNothingMore(stateIn, msgIn as EngineMessage, o)
}

private fun checkShouldDoNothing(msgIn: EngineMessage, o: EngineResults) {
    verifyOrder {
        o.storage.getState(msgIn.taskId)
    }
    checkConfirmVerified(o)
}

private fun checkShouldRetryTaskAttempt(msgIn: EngineMessage, stateIn: EngineState, o: EngineResults) {
    verifyOrder {
        o.storage.getState(msgIn.taskId)
        o.taskDispatcher.dispatch(o.runTask!!)
        o.taskDispatcher.dispatch(o.taskAttemptDispatched!!)
        o.storage.updateState(msgIn.taskId, o.state!!, stateIn)
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
    o.state!!.taskStatus shouldBe TaskStatus.RUNNING_WARNING
    o.taskStatusUpdated!!.oldStatus shouldBe stateIn.taskStatus
    o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.RUNNING_WARNING
}

private fun checkShouldWarnAndDoNothingMore(stateIn: EngineState?, msgIn: EngineMessage, o: EngineResults) {
    verifyOrder {
        o.storage.getState(msgIn.taskId)
        o.logger.warn(any(), msgIn, stateIn)
    }
    checkConfirmVerified(o)
}

private fun checkShouldErrorAndDoNothingMore(stateIn: EngineState?, msgIn: EngineMessage, o: EngineResults) {
    verifyOrder {
        o.storage.getState(msgIn.taskId)
        o.logger.error(any(), msgIn, stateIn)
    }
    checkConfirmVerified(o)
}

private fun checkConfirmVerified(o: EngineResults) {
    confirmVerified(o.taskDispatcher)
    confirmVerified(o.workflowDispatcher)
    confirmVerified(o.storage)
    confirmVerified(o.logger)
}
