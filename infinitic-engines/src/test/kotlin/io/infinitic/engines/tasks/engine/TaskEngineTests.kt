// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.engines.tasks.engine

import io.infinitic.common.SendToMonitoringPerName
import io.infinitic.common.SendToTaskEngine
import io.infinitic.common.SendToWorkers
import io.infinitic.common.data.interfaces.plus
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.monitoringPerName.messages.TaskStatusUpdated
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.messages.CancelTask
import io.infinitic.common.tasks.messages.DispatchTask
import io.infinitic.common.tasks.messages.RetryTask
import io.infinitic.common.tasks.messages.RetryTaskAttempt
import io.infinitic.common.tasks.messages.TaskAttemptCompleted
import io.infinitic.common.tasks.messages.TaskAttemptDispatched
import io.infinitic.common.tasks.messages.TaskAttemptFailed
import io.infinitic.common.tasks.messages.TaskAttemptStarted
import io.infinitic.common.tasks.messages.TaskCanceled
import io.infinitic.common.tasks.messages.TaskCompleted
import io.infinitic.common.tasks.messages.TaskEngineMessage
import io.infinitic.common.tasks.state.TaskState
import io.infinitic.common.workers.messages.RunTask
import io.infinitic.common.workers.messages.WorkerMessage
import io.infinitic.engines.tasks.storage.TaskStateStorage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
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
    lateinit var sendToTaskEngine: SendToTaskEngine
    lateinit var sendToWorkers: SendToWorkers
    lateinit var sendToMonitoringPerName: SendToMonitoringPerName
    lateinit var storage: TaskStateStorage
    lateinit var logger: Logger
    var state: TaskState? = null
    var workerMessage: WorkerMessage? = null
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

internal fun engineHandle(stateIn: TaskState?, msgIn: TaskEngineMessage): EngineResults = runBlocking {
    // deep copy of stateIn to prevent updating it
    val state: TaskState? = stateIn?.deepCopy()
    // mocking
    val logger = mockk<Logger>()
    val taskStateStorage = mockk<TaskStateStorage>()
    val sendToTaskEngine = mockk<SendToTaskEngine>()
    val sendToWorkers = mockk<SendToWorkers>()
    val sendToMonitoringPerName = mockk<SendToMonitoringPerName>()
    val stateSlot = slot<TaskState>()
    val taskAttemptCompletedSlot = slot<TaskAttemptCompleted>()
    val taskAttemptDispatchedSlot = slot<TaskAttemptDispatched>()
    val taskAttemptFailedSlot = slot<TaskAttemptFailed>()
    val taskAttemptStartedSlot = slot<TaskAttemptStarted>()
    val taskCanceledSlot = slot<TaskCanceled>()
    val taskCompletedSlot = slot<TaskCompleted>()
    val retryTaskAttemptSlot = slot<RetryTaskAttempt>()
    val retryTaskAttemptDelaySlot = slot<Float>()
    val workerMessageSlot = slot<WorkerMessage>()
    val taskStatusUpdatedSlot = slot<TaskStatusUpdated>()
    every { logger.error(any(), msgIn, stateIn) } just Runs
    every { logger.warn(any(), msgIn, stateIn) } just Runs
    every { taskStateStorage.getState(any()) } returns state
    every { taskStateStorage.updateState(any(), capture(stateSlot), any()) } just Runs
    every { taskStateStorage.deleteState(any()) } just Runs
    coEvery { sendToWorkers(capture(workerMessageSlot)) } just Runs
    coEvery { sendToTaskEngine(capture(retryTaskAttemptSlot), capture(retryTaskAttemptDelaySlot)) } just Runs
    coEvery { sendToTaskEngine(capture(taskAttemptCompletedSlot), 0F) } just Runs
    coEvery { sendToTaskEngine(capture(taskAttemptDispatchedSlot), 0F) } just Runs
    coEvery { sendToTaskEngine(capture(taskAttemptFailedSlot), 0F) } just Runs
    coEvery { sendToTaskEngine(capture(taskAttemptStartedSlot), 0F) } just Runs
    coEvery { sendToTaskEngine(capture(taskCanceledSlot), 0F) } just Runs
    coEvery { sendToTaskEngine(capture(taskCompletedSlot), 0F) } just Runs
    coEvery { sendToMonitoringPerName(capture(taskStatusUpdatedSlot)) } just Runs
    // given
    val engine = TaskEngine(taskStateStorage, sendToTaskEngine, sendToMonitoringPerName, sendToWorkers)
    // when
    engine.handle(msgIn)
    // then
    val o = EngineResults()
    o.sendToTaskEngine = sendToTaskEngine
    o.sendToWorkers = sendToWorkers
    o.sendToMonitoringPerName = sendToMonitoringPerName
    o.storage = taskStateStorage
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
            o.storage.getState(msgIn.taskId)
            o.sendToTaskEngine(o.taskCanceled!!, 0F)
            o.storage.deleteState(msgIn.taskId)
            o.sendToMonitoringPerName(o.taskStatusUpdated!!)
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
            o.storage.getState(msgIn.taskId)
            o.sendToWorkers(o.workerMessage!!)
            o.sendToTaskEngine(o.taskAttemptDispatched!!, 0F)
            o.storage.updateState(msgIn.taskId, o.state!!, null)
            o.sendToMonitoringPerName(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.workerMessage.shouldBeInstanceOf<RunTask>()
        val run = o.workerMessage as RunTask
        run.taskId shouldBe msgIn.taskId
        run.taskName shouldBe msgIn.taskName
        run.methodInput shouldBe msgIn.methodInput
        run.taskAttemptRetry.int shouldBe 0
        run.taskAttemptId shouldBe o.taskAttemptDispatched!!.taskAttemptId
        run.taskAttemptRetry shouldBe o.taskAttemptDispatched!!.taskAttemptRetry
        o.state!!.taskId shouldBe msgIn.taskId
        o.state!!.taskName shouldBe msgIn.taskName
        o.state!!.methodInput shouldBe msgIn.methodInput
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
                "methodName" to null,
                "methodParameterTypes" to null,
                "methodInput" to null,
                "taskMeta" to null,
                "taskOptions" to null
            )
        )
        val o = engineHandle(stateIn, msgIn)
        coVerifyAll {
            o.storage.getState(msgIn.taskId)
            o.sendToWorkers(o.workerMessage!!)
            o.sendToTaskEngine(o.taskAttemptDispatched!!, 0F)
            o.storage.updateState(msgIn.taskId, o.state!!, stateIn)
            o.sendToMonitoringPerName(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.workerMessage.shouldBeInstanceOf<RunTask>()
        val run = o.workerMessage as RunTask
        run.taskId shouldBe stateIn.taskId
        run.taskAttemptId shouldNotBe stateIn.taskAttemptId
        run.taskAttemptRetry.int shouldBe 0
        run.taskName shouldBe stateIn.taskName
        run.methodInput shouldBe stateIn.methodInput
        o.taskAttemptDispatched!!.taskId shouldBe stateIn.taskId
        o.taskAttemptDispatched!!.taskAttemptId shouldBe run.taskAttemptId
        o.taskAttemptDispatched!!.taskAttemptRetry.int shouldBe 0
        o.state!!.taskId shouldBe stateIn.taskId
        o.state!!.taskName shouldBe stateIn.taskName
        o.state!!.methodInput shouldBe stateIn.methodInput
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
            o.storage.getState(msgIn.taskId)
            o.sendToTaskEngine(o.taskCompleted!!, 0F)
            o.storage.deleteState(msgIn.taskId)
            o.sendToMonitoringPerName(o.taskStatusUpdated!!)
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
            o.storage.getState(msgIn.taskId)
            o.storage.updateState(msgIn.taskId, o.state!!, stateIn)
            o.sendToMonitoringPerName(o.taskStatusUpdated!!)
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
            o.storage.getState(msgIn.taskId)
            o.sendToTaskEngine(o.retryTaskAttempt!!, o.retryTaskAttemptDelay!!)
            o.storage.updateState(msgIn.taskId, o.state!!, stateIn)
            o.sendToMonitoringPerName(o.taskStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.retryTaskAttempt!!.taskId shouldBe stateIn.taskId
        o.retryTaskAttempt!!.taskAttemptId shouldBe stateIn.taskAttemptId
        o.retryTaskAttempt!!.taskAttemptRetry shouldBe stateIn.taskAttemptRetry
        o.retryTaskAttemptDelay!! shouldBe msgIn.taskAttemptDelayBeforeRetry
        o.taskStatusUpdated!!.taskId shouldBe stateIn.taskId
        o.taskStatusUpdated!!.taskName shouldBe TaskName("${stateIn.taskName}::${stateIn.methodName}")
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

private fun checkShouldRetryTaskAttempt(msgIn: TaskEngineMessage, stateIn: TaskState, o: EngineResults) {
    coVerifyOrder {
        o.storage.getState(msgIn.taskId)
        o.sendToWorkers(o.workerMessage!!)
        o.sendToTaskEngine(o.taskAttemptDispatched!!, 0F)
        o.storage.updateState(msgIn.taskId, o.state!!, stateIn)
        o.sendToMonitoringPerName(o.taskStatusUpdated!!)
    }
    checkConfirmVerified(o)
    o.workerMessage.shouldBeInstanceOf<RunTask>()
    val run = o.workerMessage as RunTask
    run.taskId shouldBe stateIn.taskId
    run.taskAttemptId shouldBe stateIn.taskAttemptId
    run.taskAttemptRetry shouldBe stateIn.taskAttemptRetry + 1
    run.taskName shouldBe stateIn.taskName
    run.methodInput shouldBe stateIn.methodInput
    o.taskAttemptDispatched!!.taskId shouldBe stateIn.taskId
    o.taskAttemptDispatched!!.taskAttemptId shouldBe run.taskAttemptId
    o.taskAttemptDispatched!!.taskAttemptRetry shouldBe run.taskAttemptRetry
    o.state!!.taskId shouldBe stateIn.taskId
    o.state!!.taskName shouldBe stateIn.taskName
    o.state!!.methodInput shouldBe stateIn.methodInput
    o.state!!.taskAttemptId shouldBe run.taskAttemptId
    o.state!!.taskAttemptRetry shouldBe run.taskAttemptRetry
    o.state!!.taskStatus shouldBe TaskStatus.RUNNING_WARNING
    o.taskStatusUpdated!!.oldStatus shouldBe stateIn.taskStatus
    o.taskStatusUpdated!!.newStatus shouldBe TaskStatus.RUNNING_WARNING
}

private fun checkConfirmVerified(o: EngineResults) {
    confirmVerified(o.sendToWorkers)
    confirmVerified(o.sendToTaskEngine)
    confirmVerified(o.sendToMonitoringPerName)
    confirmVerified(o.storage)
    confirmVerified(o.logger)
}

private fun state(values: Map<String, Any?>? = null) = TestFactory.random<TaskState>(values)
private fun cancelTask(values: Map<String, Any?>? = null) = TestFactory.random<CancelTask>(values)
private fun dispatchTask(values: Map<String, Any?>? = null) = TestFactory.random<DispatchTask>(values)
private fun retryTask(values: Map<String, Any?>? = null) = TestFactory.random<RetryTask>(values)
private fun retryTaskAttempt(values: Map<String, Any?>? = null) = TestFactory.random<RetryTaskAttempt>(values)
private fun taskCompleted(values: Map<String, Any?>? = null) = TestFactory.random<TaskCompleted>(values)
private fun taskCanceled(values: Map<String, Any?>? = null) = TestFactory.random<TaskCanceled>(values)
private fun taskAttemptDispatched(values: Map<String, Any?>? = null) = TestFactory.random<TaskAttemptDispatched>(values)
private fun taskAttemptCompleted(values: Map<String, Any?>? = null) = TestFactory.random<TaskAttemptCompleted>(values)
private fun taskAttemptFailed(values: Map<String, Any?>? = null) = TestFactory.random<TaskAttemptFailed>(values)
private fun taskAttemptStarted(values: Map<String, Any?>? = null) = TestFactory.random<TaskAttemptStarted>(values)
