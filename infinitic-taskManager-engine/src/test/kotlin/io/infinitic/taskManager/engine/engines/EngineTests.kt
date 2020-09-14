package io.infinitic.taskManager.engine.engines

import io.infinitic.common.data.interfaces.plus
import io.infinitic.taskManager.common.data.TaskStatus
import io.infinitic.taskManager.engine.dispatcher.Dispatcher
import io.infinitic.taskManager.common.messages.CancelTask
import io.infinitic.taskManager.common.messages.DispatchTask
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage
import io.infinitic.taskManager.common.messages.ForWorkerMessage
import io.infinitic.taskManager.common.messages.TaskAttemptCompleted
import io.infinitic.taskManager.common.messages.TaskAttemptDispatched
import io.infinitic.taskManager.common.messages.TaskAttemptFailed
import io.infinitic.taskManager.common.messages.TaskAttemptStarted
import io.infinitic.taskManager.common.messages.TaskCanceled
import io.infinitic.taskManager.common.messages.TaskCompleted
import io.infinitic.taskManager.common.messages.TaskStatusUpdated
import io.infinitic.taskManager.common.messages.RetryTask
import io.infinitic.taskManager.common.messages.RetryTaskAttempt
import io.infinitic.taskManager.common.messages.RunTask
import io.infinitic.taskManager.common.states.TaskEngineState
import io.infinitic.taskManager.engine.storage.StateStorage
import io.infinitic.taskManager.engine.utils.TestFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.coVerifyOrder
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger

internal class EngineResults {
    lateinit var dispatcher: Dispatcher
    lateinit var storage: StateStorage
    lateinit var logger: Logger
    var state: TaskEngineState? = null
    var workerMessage: ForWorkerMessage? = null
    var retryTaskAttempt: RetryTaskAttempt? = null
    var retryTaskAttemptDelay: Float? = null
    var taskAttemptCompleted: TaskAttemptCompleted? = null
    var taskAttemptDispatched: TaskAttemptDispatched? = null
    var taskAttemptFailed: TaskAttemptFailed? = null
    var taskAttemptStarted: TaskAttemptStarted? = null
    var taskCanceled: TaskCanceled? = null
    var taskCompleted: TaskCompleted? = null
    var taskStatusUpdated: TaskStatusUpdated? = null
}

internal fun engineHandle(stateIn: TaskEngineState?, msgIn: ForTaskEngineMessage): EngineResults = runBlocking {
    // deep copy of stateIn to avoid updating it
    val state: TaskEngineState? = stateIn?.deepCopy()
    // mocking
    val logger = mockk<Logger>()
    val storage = mockk<StateStorage>()
    val dispatcher = mockk<Dispatcher>()
    val stateSlot = slot<TaskEngineState>()
    val taskAttemptCompletedSlot = slot<TaskAttemptCompleted>()
    val taskAttemptDispatchedSlot = slot<TaskAttemptDispatched>()
    val taskAttemptFailedSlot = slot<TaskAttemptFailed>()
    val taskAttemptStartedSlot = slot<TaskAttemptStarted>()
    val taskCanceledSlot = slot<TaskCanceled>()
    val taskCompletedSlot = slot<TaskCompleted>()
    val retryTaskAttemptSlot = slot<RetryTaskAttempt>()
    val retryTaskAttemptDelaySlot = slot<Float>()
    val workerMessageSlot = slot<ForWorkerMessage>()
    val taskStatusUpdatedSlot = slot<TaskStatusUpdated>()
    every { logger.error(any(), msgIn, stateIn) } just Runs
    every { logger.warn(any(), msgIn, stateIn) } just Runs
    every { storage.getTaskEngineState(any()) } returns state
    every { storage.updateTaskEngineState(any(), capture(stateSlot), any()) } just Runs
    every { storage.deleteTaskEngineState(any()) } just Runs
    coEvery { dispatcher.toWorkers(capture(workerMessageSlot)) } just Runs
    coEvery { dispatcher.toTaskEngine(capture(retryTaskAttemptSlot), capture(retryTaskAttemptDelaySlot)) } just Runs
    coEvery { dispatcher.toTaskEngine(capture(taskAttemptCompletedSlot)) } just Runs
    coEvery { dispatcher.toTaskEngine(capture(taskAttemptDispatchedSlot)) } just Runs
    coEvery { dispatcher.toTaskEngine(capture(taskAttemptFailedSlot)) } just Runs
    coEvery { dispatcher.toTaskEngine(capture(taskAttemptStartedSlot)) } just Runs
    coEvery { dispatcher.toTaskEngine(capture(taskCanceledSlot)) } just Runs
    coEvery { dispatcher.toTaskEngine(capture(taskCompletedSlot)) } just Runs
    coEvery { dispatcher.toMonitoringPerName(capture(taskStatusUpdatedSlot)) } just Runs
    // given
    val engine = TaskEngine(storage, dispatcher)
    // when
    engine.handle(msgIn)
    // then
    val o = EngineResults()
    o.dispatcher = dispatcher
    o.storage = storage
    o.logger = logger
    if (stateSlot.isCaptured) o.state = stateSlot.captured
    if (workerMessageSlot.isCaptured) o.workerMessage = workerMessageSlot.captured
    if (retryTaskAttemptSlot.isCaptured) o.retryTaskAttempt = retryTaskAttemptSlot.captured
    if (retryTaskAttemptDelaySlot.isCaptured) o.retryTaskAttemptDelay = retryTaskAttemptDelaySlot.captured
    if (taskAttemptCompletedSlot.isCaptured) o.taskAttemptCompleted = taskAttemptCompletedSlot.captured
    if (taskAttemptDispatchedSlot.isCaptured) o.taskAttemptDispatched = taskAttemptDispatchedSlot.captured
    if (taskAttemptFailedSlot.isCaptured) o.taskAttemptFailed = taskAttemptFailedSlot.captured
    if (taskAttemptStartedSlot.isCaptured) o.taskAttemptStarted = taskAttemptStartedSlot.captured
    if (taskCanceledSlot.isCaptured) o.taskCanceled = taskCanceledSlot.captured
    if (taskCompletedSlot.isCaptured) o.taskCompleted = taskCompletedSlot.captured
    if (taskStatusUpdatedSlot.isCaptured) o.taskStatusUpdated = taskStatusUpdatedSlot.captured

    return@runBlocking o
}

internal class TaskEngineTests : StringSpec({
    "TaskAttemptDispatched" {
        val stateIn = state()
        val msgIn = taskAttemptDispatched()
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(o)
    }

    "TaskAttemptStarted" {
        val stateIn = state()
        val msgIn = taskAttemptStarted()
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(o)
    }

    "TaskCompleted" {
        val stateIn = state()
        val msgIn = taskCompleted()
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(o)
    }

    "TaskCanceled" {
        val stateIn = state()
        val msgIn = taskCanceled()
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(o)
    }

    "CancelTask" {
        val stateIn = state(mapOf("taskStatus" to TaskStatus.RUNNING_OK))
        val msgIn = cancelTask(mapOf("taskId" to stateIn.taskId))
        val o = engineHandle(stateIn, msgIn)
        coVerifyOrder {
            o.storage.getTaskEngineState(msgIn.taskId)
            o.dispatcher.toTaskEngine(o.taskCanceled!!)
            o.storage.deleteTaskEngineState(msgIn.taskId)
            o.dispatcher.toMonitoringPerName(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.taskCanceled!!.taskId shouldBe msgIn.taskId
        o.taskCanceled!!.taskMeta shouldBe stateIn.taskMeta
        o.taskStatusUpdated!!.oldStatus shouldBe stateIn.taskStatus
        o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.TERMINATED_CANCELED
    }

    "DispatchTask" {
        val msgIn = dispatchTask()
        val o = engineHandle(null, msgIn)
        coVerifyOrder {
            o.storage.getTaskEngineState(msgIn.taskId)
            o.dispatcher.toWorkers(o.workerMessage!!)
            o.dispatcher.toTaskEngine(o.taskAttemptDispatched!!)
            o.storage.updateTaskEngineState(msgIn.taskId, o.state!!, null)
            o.dispatcher.toMonitoringPerName(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
        (o.workerMessage is RunTask) shouldBe true
        val run = o.workerMessage as RunTask
        run.taskId shouldBe msgIn.taskId
        run.taskName shouldBe msgIn.taskName
        run.taskInput shouldBe msgIn.taskInput
        run.taskAttemptRetry.int shouldBe 0
        run.taskAttemptId shouldBe o.taskAttemptDispatched!!.taskAttemptId
        run.taskAttemptRetry shouldBe o.taskAttemptDispatched!!.taskAttemptRetry
        o.state!!.taskId shouldBe msgIn.taskId
        o.state!!.taskName shouldBe msgIn.taskName
        o.state!!.taskInput shouldBe msgIn.taskInput
        o.state!!.taskAttemptId shouldBe run.taskAttemptId
        o.state!!.taskAttemptRetry.int shouldBe 0
        o.state!!.taskMeta shouldBe msgIn.taskMeta
        o.state!!.taskStatus shouldBe TaskStatus.RUNNING_OK
        o.taskStatusUpdated!!.oldStatus shouldBe null
        o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.RUNNING_OK
    }

    "RetryTask" {
        val stateIn = state(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_ERROR
            )
        )
        val msgIn = retryTask(
            mapOf(
                "taskId" to stateIn.taskId,
                "taskName" to null,
                "taskInput" to null,
                "taskMeta" to null,
                "taskOptions" to null
            )
        )
        val o = engineHandle(stateIn, msgIn)
        coVerifyAll {
            o.storage.getTaskEngineState(msgIn.taskId)
            o.dispatcher.toWorkers(o.workerMessage!!)
            o.dispatcher.toTaskEngine(o.taskAttemptDispatched!!)
            o.storage.updateTaskEngineState(msgIn.taskId, o.state!!, stateIn)
            o.dispatcher.toMonitoringPerName(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
        (o.workerMessage is RunTask) shouldBe true
        val run = o.workerMessage as RunTask
        run.taskId shouldBe stateIn.taskId
        run.taskAttemptId shouldNotBe stateIn.taskAttemptId
        run.taskAttemptRetry.int shouldBe 0
        run.taskName shouldBe stateIn.taskName
        run.taskInput shouldBe stateIn.taskInput
        o.taskAttemptDispatched!!.taskId shouldBe stateIn.taskId
        o.taskAttemptDispatched!!.taskAttemptId shouldBe run.taskAttemptId
        o.taskAttemptDispatched!!.taskAttemptRetry.int shouldBe 0
        o.state!!.taskId shouldBe stateIn.taskId
        o.state!!.taskName shouldBe stateIn.taskName
        o.state!!.taskInput shouldBe stateIn.taskInput
        o.state!!.taskAttemptId shouldBe run.taskAttemptId
        o.state!!.taskAttemptRetry shouldBe run.taskAttemptRetry
        o.state!!.taskStatus shouldBe TaskStatus.RUNNING_WARNING
        o.taskStatusUpdated!!.oldStatus shouldBe stateIn.taskStatus
        o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.RUNNING_WARNING
    }

    "RetryTaskAttempt" {
        val stateIn = state(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_ERROR
            )
        )
        val msgIn = retryTaskAttempt(
            mapOf(
                "taskId" to stateIn.taskId,
                "taskAttemptId" to stateIn.taskAttemptId,
                "taskAttemptRetry" to stateIn.taskAttemptRetry
            )
        )
        val o = engineHandle(stateIn, msgIn)
        checkShouldRetryTaskAttempt(msgIn, stateIn, o)
    }

    "TaskAttemptCompleted" {
        val stateIn = state(mapOf("taskStatus" to TaskStatus.RUNNING_OK))
        val msgIn = taskAttemptCompleted(mapOf("taskId" to stateIn.taskId))
        val o = engineHandle(stateIn, msgIn)
        coVerifyOrder {
            o.storage.getTaskEngineState(msgIn.taskId)
            o.dispatcher.toTaskEngine(o.taskCompleted!!)
            o.storage.deleteTaskEngineState(msgIn.taskId)
            o.dispatcher.toMonitoringPerName(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.taskStatusUpdated!!.oldStatus shouldBe stateIn.taskStatus
        o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        o.taskCompleted!!.taskOutput shouldBe msgIn.taskOutput
        o.taskCompleted!!.taskMeta shouldBe stateIn.taskMeta
    }

    "TaskAttemptFailed without retry" {
        val stateIn = state(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_OK
            )
        )
        val msgIn = taskAttemptFailed(
            mapOf(
                "taskId" to stateIn.taskId,
                "taskAttemptId" to stateIn.taskAttemptId,
                "taskAttemptRetry" to stateIn.taskAttemptRetry,
                "taskAttemptDelayBeforeRetry" to null
            )
        )
        val o = engineHandle(stateIn, msgIn)
        coVerifyOrder {
            o.storage.getTaskEngineState(msgIn.taskId)
            o.storage.updateTaskEngineState(msgIn.taskId, o.state!!, stateIn)
            o.dispatcher.toMonitoringPerName(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
    }

    "TaskAttemptFailed with future retry" {
        val stateIn = state(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_OK
            )
        )
        val msgIn = taskAttemptFailed(
            mapOf(
                "taskId" to stateIn.taskId,
                "taskAttemptId" to stateIn.taskAttemptId,
                "taskAttemptRetry" to stateIn.taskAttemptRetry,
                "taskAttemptDelayBeforeRetry" to 42F
            )
        )
        val o = engineHandle(stateIn, msgIn)
        coVerifyOrder {
            o.storage.getTaskEngineState(msgIn.taskId)
            o.dispatcher.toTaskEngine(o.retryTaskAttempt!!, o.retryTaskAttemptDelay!!)
            o.storage.updateTaskEngineState(msgIn.taskId, o.state!!, stateIn)
            o.dispatcher.toMonitoringPerName(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.retryTaskAttempt!!.taskId shouldBe stateIn.taskId
        o.retryTaskAttempt!!.taskAttemptId shouldBe stateIn.taskAttemptId
        o.retryTaskAttempt!!.taskAttemptRetry shouldBe stateIn.taskAttemptRetry
        o.retryTaskAttemptDelay!! shouldBe msgIn.taskAttemptDelayBeforeRetry
        o.taskStatusUpdated!!.taskId shouldBe stateIn.taskId
        o.taskStatusUpdated!!.taskName shouldBe stateIn.taskName
        o.taskStatusUpdated!!.oldStatus shouldBe stateIn.taskStatus
        o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.RUNNING_WARNING
    }

    "TaskAttemptFailed with immediate retry" {
        val stateIn = state(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_OK
            )
        )
        val msgIn = taskAttemptFailed(
            mapOf(
                "taskId" to stateIn.taskId,
                "taskAttemptId" to stateIn.taskAttemptId,
                "taskAttemptRetry" to stateIn.taskAttemptRetry,
                "taskAttemptDelayBeforeRetry" to 0F
            )
        )
        val o = engineHandle(stateIn, msgIn)
        checkShouldRetryTaskAttempt(msgIn, stateIn, o)
    }

    "TaskAttemptFailed with immediate retry (negative delay)" {
        val stateIn = state(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_OK
            )
        )
        val msgIn = taskAttemptFailed(
            mapOf(
                "taskId" to stateIn.taskId,
                "taskAttemptId" to stateIn.taskAttemptId,
                "taskAttemptRetry" to stateIn.taskAttemptRetry,
                "taskAttemptDelayBeforeRetry" to -42F
            )
        )
        val o = engineHandle(stateIn, msgIn)
        checkShouldRetryTaskAttempt(msgIn, stateIn, o)
    }

    // TODO: add tests for retryTask with non-null parameters
})

private fun checkShouldDoNothing(o: EngineResults) {
    checkConfirmVerified(o)
}

private fun checkShouldRetryTaskAttempt(msgIn: ForTaskEngineMessage, stateIn: TaskEngineState, o: EngineResults) {
    coVerifyOrder {
        o.storage.getTaskEngineState(msgIn.taskId)
        o.dispatcher.toWorkers(o.workerMessage!!)
        o.dispatcher.toTaskEngine(o.taskAttemptDispatched!!)
        o.storage.updateTaskEngineState(msgIn.taskId, o.state!!, stateIn)
        o.dispatcher.toMonitoringPerName(o.taskStatusUpdated!!)
    }
    checkConfirmVerified(o)
    (o.workerMessage is RunTask) shouldBe true
    val run = o.workerMessage as RunTask
    run.taskId shouldBe stateIn.taskId
    run.taskAttemptId shouldBe stateIn.taskAttemptId
    run.taskAttemptRetry shouldBe stateIn.taskAttemptRetry + 1
    run.taskName shouldBe stateIn.taskName
    run.taskInput shouldBe stateIn.taskInput
    o.taskAttemptDispatched!!.taskId shouldBe stateIn.taskId
    o.taskAttemptDispatched!!.taskAttemptId shouldBe run.taskAttemptId
    o.taskAttemptDispatched!!.taskAttemptRetry shouldBe run.taskAttemptRetry
    o.state!!.taskId shouldBe stateIn.taskId
    o.state!!.taskName shouldBe stateIn.taskName
    o.state!!.taskInput shouldBe stateIn.taskInput
    o.state!!.taskAttemptId shouldBe run.taskAttemptId
    o.state!!.taskAttemptRetry shouldBe run.taskAttemptRetry
    o.state!!.taskStatus shouldBe TaskStatus.RUNNING_WARNING
    o.taskStatusUpdated!!.oldStatus shouldBe stateIn.taskStatus
    o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.RUNNING_WARNING
}

private fun checkConfirmVerified(o: EngineResults) {
    confirmVerified(o.dispatcher)
    confirmVerified(o.storage)
    confirmVerified(o.logger)
}

private fun state(values: Map<String, Any?>? = null) = TestFactory.random(TaskEngineState::class, values)
private fun cancelTask(values: Map<String, Any?>? = null) = TestFactory.random(CancelTask::class, values)
private fun dispatchTask(values: Map<String, Any?>? = null) = TestFactory.random(DispatchTask::class, values)
private fun retryTask(values: Map<String, Any?>? = null) = TestFactory.random(RetryTask::class, values)
private fun retryTaskAttempt(values: Map<String, Any?>? = null) = TestFactory.random(RetryTaskAttempt::class, values)
private fun taskCompleted(values: Map<String, Any?>? = null) = TestFactory.random(TaskCompleted::class, values)
private fun taskCanceled(values: Map<String, Any?>? = null) = TestFactory.random(TaskCanceled::class, values)
private fun taskAttemptDispatched(values: Map<String, Any?>? = null) = TestFactory.random(TaskAttemptDispatched::class, values)
private fun taskAttemptCompleted(values: Map<String, Any?>? = null) = TestFactory.random(TaskAttemptCompleted::class, values)
private fun taskAttemptFailed(values: Map<String, Any?>? = null) = TestFactory.random(TaskAttemptFailed::class, values)
private fun taskAttemptStarted(values: Map<String, Any?>? = null) = TestFactory.random(TaskAttemptStarted::class, values)
