/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.tasks.engine

import io.infinitic.common.data.interfaces.plus
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.monitoring.perName.messages.TaskStatusUpdated
import io.infinitic.common.monitoring.perName.transport.SendToMonitoringPerName
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.tasks.engine.messages.RetryTaskAttempt
import io.infinitic.common.tasks.engine.messages.TaskAttemptCompleted
import io.infinitic.common.tasks.engine.messages.TaskAttemptDispatched
import io.infinitic.common.tasks.engine.messages.TaskAttemptFailed
import io.infinitic.common.tasks.engine.messages.TaskAttemptStarted
import io.infinitic.common.tasks.engine.messages.TaskCanceled
import io.infinitic.common.tasks.engine.messages.TaskCompleted
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.state.TaskState
import io.infinitic.common.tasks.engine.storage.InsertTaskEvent
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.tasks.executors.SendToExecutors
import io.infinitic.common.tasks.executors.messages.RunTask
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.infinitic.tasks.engine.storage.events.TaskEventStorage
import io.infinitic.tasks.engine.storage.states.DeleteTaskState
import io.infinitic.tasks.engine.storage.states.GetTaskState
import io.infinitic.tasks.engine.storage.states.TaskStateStorage
import io.infinitic.tasks.engine.storage.states.UpdateTaskState
import io.infinitic.tasks.engine.transport.TaskEngineOutput
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import org.slf4j.Logger

fun <T : Any> captured(slot: CapturingSlot<T>) = if (slot.isCaptured) slot.captured else null

class MockTaskStateStorage(private val state: TaskState?) : TaskStateStorage {
    override val getState = mockk<GetTaskState>()
    override val updateState = mockk<UpdateTaskState>()
    override val deleteState = mockk<DeleteTaskState>()

    val stateSlot = slot<TaskState>()

    init {
        coEvery { getState(any()) } returns state?.deepCopy()
        coEvery { updateState(any(), capture(stateSlot), any()) } just Runs
        coEvery { deleteState(any()) } just Runs
    }
}

class MockTaskEventStorage() : TaskEventStorage {
    override val insertTaskEvent = mockk<InsertTaskEvent>()

    val dispatchTaskSlot = slot<DispatchTask>()
    val cancelTaskSlot = slot<CancelTask>()
    val taskCanceledSlot = slot<TaskCanceled>()
    val taskCompletedSlot = slot<TaskCompleted>()
    val retryTaskSlot = slot<RetryTask>()
    val taskAttemptDispatchedSlot = slot<TaskAttemptDispatched>()
    val taskAttemptStartedSlot = slot<TaskAttemptStarted>()
    val taskAttemptCompletedSlot = slot<TaskAttemptCompleted>()
    val taskAttemptFailedSlot = slot<TaskAttemptFailed>()
    val retryTaskAttemptSlot = slot<RetryTaskAttempt>()

    init {
        coEvery { insertTaskEvent(capture(dispatchTaskSlot)) } just Runs
        coEvery { insertTaskEvent(capture(cancelTaskSlot)) } just Runs
        coEvery { insertTaskEvent(capture(taskCanceledSlot)) } just Runs
        coEvery { insertTaskEvent(capture(taskCompletedSlot)) } just Runs
        coEvery { insertTaskEvent(capture(retryTaskSlot)) } just Runs
        coEvery { insertTaskEvent(capture(taskAttemptDispatchedSlot)) } just Runs
        coEvery { insertTaskEvent(capture(taskAttemptStartedSlot)) } just Runs
        coEvery { insertTaskEvent(capture(taskAttemptCompletedSlot)) } just Runs
        coEvery { insertTaskEvent(capture(taskAttemptFailedSlot)) } just Runs
        coEvery { insertTaskEvent(capture(retryTaskAttemptSlot)) } just Runs
    }
}

class MockTaskEngineOutput : TaskEngineOutput {
    override val sendToWorkflowEngine = mockk<SendToWorkflowEngine>()
    override val sendToTaskEngine = mockk<SendToTaskEngine>()
    override val sendToTaskExecutors = mockk<SendToExecutors>()
    override val sendToMonitoringPerName = mockk<SendToMonitoringPerName>()

    val workerMessageSlot = slot<TaskExecutorMessage>()
    val retryTaskAttemptSlot = slot<RetryTaskAttempt>()
    val retryTaskAttemptDelaySlot = slot<Float>()
    val taskStatusUpdatedSlot = slot<TaskStatusUpdated>()
    val workflowMessageSlot = slot<WorkflowEngineMessage>()
    val taskCanceledSlot = slot<TaskCanceled>()
    val taskAttemptDispatchedSlot = slot<TaskAttemptDispatched>()
    val taskCompletedSlot = slot<TaskCompleted>()
    init {
        coEvery { sendToTaskExecutors(capture(workerMessageSlot)) } just Runs
        coEvery { sendToMonitoringPerName(capture(taskStatusUpdatedSlot)) } just Runs
        coEvery { sendToTaskEngine(capture(retryTaskAttemptSlot), capture(retryTaskAttemptDelaySlot)) } just Runs
        coEvery { sendToTaskEngine(capture(taskCanceledSlot), 0F) } just Runs
        coEvery { sendToTaskEngine(capture(taskAttemptDispatchedSlot), 0F) } just Runs
        coEvery { sendToTaskEngine(capture(taskCompletedSlot), 0F) } just Runs
        coEvery { sendToWorkflowEngine(capture(workflowMessageSlot), any()) } just Runs
    }
}
class TestEngine(private val stateIn: TaskState?, private val msgIn: TaskEngineMessage) {
    // mocking Logger
    private val logger = mockk<Logger>()
    // mocking TaskStateStorage
    val taskStateStorage = MockTaskStateStorage(stateIn)
    // mocking TaskEventStorage
    val taskEventStorage = MockTaskEventStorage()
    // mocking TaskEngineOutput
    val taskEngineOutput = MockTaskEngineOutput()

    init {
        every { logger.error(any(), msgIn, stateIn) } just Runs
        every { logger.warn(any(), msgIn, stateIn) } just Runs
    }

    suspend fun handle() {
        val engine = TaskEngine(taskStateStorage, taskEventStorage, taskEngineOutput)

        engine.handle(msgIn)
    }
}

internal class TaskEngineTests : StringSpec({
    "CancelTask" {
        val stateIn = state(mapOf("taskStatus" to TaskStatus.RUNNING_OK))
        val msgIn = cancelTask(mapOf("taskId" to stateIn.taskId))
        val test = TestEngine(stateIn, msgIn)
        val taskStateStorage = test.taskStateStorage
        val taskEventStorage = test.taskEventStorage
        val taskEngineOutput = test.taskEngineOutput
        test.handle()
        val taskCanceled = captured(taskEngineOutput.taskCanceledSlot)!!
        val taskStatusUpdated = captured(taskEngineOutput.taskStatusUpdatedSlot)!!
        coVerifySequence {
            taskEventStorage.insertTaskEvent(captured(taskEventStorage.cancelTaskSlot)!!)
            taskStateStorage.getState(msgIn.taskId)
            taskEngineOutput.sendToTaskEngine(taskCanceled, 0F)
            taskStateStorage.deleteState(msgIn.taskId)
            taskEngineOutput.sendToMonitoringPerName(taskStatusUpdated)
        }
        taskCanceled.taskId shouldBe msgIn.taskId
        taskCanceled.taskMeta shouldBe stateIn.taskMeta
        taskStatusUpdated.oldStatus shouldBe stateIn.taskStatus
        taskStatusUpdated.newStatus shouldBe TaskStatus.TERMINATED_CANCELED
    }

    "DispatchTask" {
        val msgIn = dispatchTask()
        val test = TestEngine(null, msgIn)
        val taskStateStorage = test.taskStateStorage
        val taskEventStorage = test.taskEventStorage
        val taskEngineOutput = test.taskEngineOutput
        test.handle()
        val runTask = captured(taskEngineOutput.workerMessageSlot)!!
        val state = captured(taskStateStorage.stateSlot)!!
        val taskStatusUpdated = captured(taskEngineOutput.taskStatusUpdatedSlot)!!
        val taskAttemptDispatched = captured(taskEngineOutput.taskAttemptDispatchedSlot)!!
        coVerifySequence {
            taskEventStorage.insertTaskEvent(captured(taskEventStorage.dispatchTaskSlot)!!)
            taskStateStorage.getState(msgIn.taskId)
            taskEngineOutput.sendToTaskExecutors(runTask)
            taskEngineOutput.sendToTaskEngine(taskAttemptDispatched, 0F)
            taskStateStorage.updateState(msgIn.taskId, state, null)
            taskEngineOutput.sendToMonitoringPerName(taskStatusUpdated)
        }
        runTask.shouldBeInstanceOf<RunTask>()
        runTask.taskId shouldBe msgIn.taskId
        runTask.taskName shouldBe msgIn.taskName
        runTask.methodInput shouldBe msgIn.methodInput
        runTask.taskAttemptRetry.int shouldBe 0
        runTask.taskAttemptId shouldBe taskAttemptDispatched.taskAttemptId
        runTask.taskAttemptRetry shouldBe taskAttemptDispatched.taskAttemptRetry
        state.taskId shouldBe msgIn.taskId
        state.taskName shouldBe msgIn.taskName
        state.methodInput shouldBe msgIn.methodInput
        state.taskAttemptId shouldBe runTask.taskAttemptId
        state.taskAttemptRetry.int shouldBe 0
        state.taskMeta shouldBe msgIn.taskMeta
        state.taskStatus shouldBe TaskStatus.RUNNING_OK
        taskStatusUpdated.oldStatus shouldBe null
        taskStatusUpdated.newStatus shouldBe TaskStatus.RUNNING_OK
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
        val test = TestEngine(stateIn, msgIn)
        val taskStateStorage = test.taskStateStorage
        val taskEventStorage = test.taskEventStorage
        val taskEngineOutput = test.taskEngineOutput
        test.handle()
        val runTask = captured(taskEngineOutput.workerMessageSlot)!!
        val taskAttemptDispatched = captured(taskEngineOutput.taskAttemptDispatchedSlot)!!
        val state = captured(taskStateStorage.stateSlot)!!
        val taskStatusUpdated = captured(taskEngineOutput.taskStatusUpdatedSlot)!!
        coVerifySequence {
            taskEventStorage.insertTaskEvent(captured(taskEventStorage.retryTaskSlot)!!)
            taskStateStorage.getState(msgIn.taskId)
            taskEngineOutput.sendToTaskExecutors(runTask)
            taskEngineOutput.sendToTaskEngine(taskAttemptDispatched, 0F)
            taskStateStorage.updateState(msgIn.taskId, state, stateIn)
            taskEngineOutput.sendToMonitoringPerName(taskStatusUpdated)
        }
        runTask.shouldBeInstanceOf<RunTask>()
        runTask.taskId shouldBe stateIn.taskId
        runTask.taskAttemptId shouldNotBe stateIn.taskAttemptId
        runTask.taskAttemptRetry.int shouldBe 0
        runTask.taskName shouldBe stateIn.taskName
        runTask.methodInput shouldBe stateIn.methodInput
        taskAttemptDispatched.taskId shouldBe stateIn.taskId
        taskAttemptDispatched.taskAttemptId shouldBe runTask.taskAttemptId
        taskAttemptDispatched.taskAttemptRetry.int shouldBe 0
        state.taskId shouldBe stateIn.taskId
        state.taskName shouldBe stateIn.taskName
        state.methodInput shouldBe stateIn.methodInput
        state.taskAttemptId shouldBe runTask.taskAttemptId
        state.taskAttemptRetry shouldBe runTask.taskAttemptRetry
        state.taskStatus shouldBe TaskStatus.RUNNING_WARNING
        taskStatusUpdated.oldStatus shouldBe stateIn.taskStatus
        taskStatusUpdated.newStatus shouldBe TaskStatus.RUNNING_WARNING
    }

    "TaskAttemptStarted" {
        val stateIn = state()
        val msgIn = taskAttemptStarted()
        val test = TestEngine(stateIn, msgIn)
        val taskEventStorage = test.taskEventStorage
        test.handle()
        coVerifySequence {
            taskEventStorage.insertTaskEvent(captured(taskEventStorage.taskAttemptStartedSlot)!!)
        }
    }

    "TaskAttemptCompleted" {
        val stateIn = state(mapOf("taskStatus" to TaskStatus.RUNNING_OK))
        val msgIn = taskAttemptCompleted(mapOf("taskId" to stateIn.taskId))

        val test = TestEngine(stateIn, msgIn)
        val taskStateStorage = test.taskStateStorage
        val taskEventStorage = test.taskEventStorage
        val taskEngineOutput = test.taskEngineOutput
        test.handle()

        val taskStatusUpdated = captured(taskEngineOutput.taskStatusUpdatedSlot)!!
        val taskCompleted = captured(taskEngineOutput.taskCompletedSlot)!!

        coVerifySequence {
            taskEventStorage.insertTaskEvent(captured(taskEventStorage.taskAttemptCompletedSlot)!!)
            taskStateStorage.getState(msgIn.taskId)
            taskEngineOutput.sendToTaskEngine(taskCompleted, 0F)
            taskStateStorage.deleteState(msgIn.taskId)
            taskEngineOutput.sendToMonitoringPerName(taskStatusUpdated)
        }
        taskStatusUpdated.oldStatus shouldBe stateIn.taskStatus
        taskStatusUpdated.newStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        taskCompleted.taskOutput shouldBe msgIn.taskOutput
        taskCompleted.taskMeta shouldBe stateIn.taskMeta
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
        val test = TestEngine(stateIn, msgIn)
        val taskStateStorage = test.taskStateStorage
        val taskEventStorage = test.taskEventStorage
        val taskEngineOutput = test.taskEngineOutput
        test.handle()

        val state = captured(taskStateStorage.stateSlot)!!
        val taskStatusUpdated = captured(taskEngineOutput.taskStatusUpdatedSlot)!!
        coVerifySequence {
            taskEventStorage.insertTaskEvent(captured(taskEventStorage.taskAttemptFailedSlot)!!)
            taskStateStorage.getState(msgIn.taskId)
            taskStateStorage.updateState(msgIn.taskId, state, stateIn)
            taskEngineOutput.sendToMonitoringPerName(taskStatusUpdated)
        }
        taskStatusUpdated.taskId shouldBe stateIn.taskId
        taskStatusUpdated.taskName shouldBe TaskName("${stateIn.taskName}::${stateIn.methodName}")
        taskStatusUpdated.oldStatus shouldBe stateIn.taskStatus
        taskStatusUpdated.newStatus shouldBe TaskStatus.RUNNING_ERROR
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
        val test = TestEngine(stateIn, msgIn)
        val taskStateStorage = test.taskStateStorage
        val taskEventStorage = test.taskEventStorage
        val taskEngineOutput = test.taskEngineOutput
        test.handle()

        val state = captured(taskStateStorage.stateSlot)!!
        val taskStatusUpdated = captured(taskEngineOutput.taskStatusUpdatedSlot)!!
        val retryTaskAttempt = captured(taskEngineOutput.retryTaskAttemptSlot)!!
        val retryTaskAttemptDelay = captured(taskEngineOutput.retryTaskAttemptDelaySlot)!!

        coVerifySequence {
            taskEventStorage.insertTaskEvent(captured(taskEventStorage.taskAttemptFailedSlot)!!)
            taskStateStorage.getState(msgIn.taskId)
            taskEngineOutput.sendToTaskEngine(retryTaskAttempt, retryTaskAttemptDelay)
            taskStateStorage.updateState(msgIn.taskId, state, stateIn)
            taskEngineOutput.sendToMonitoringPerName(taskStatusUpdated)
        }
        retryTaskAttempt.taskId shouldBe stateIn.taskId
        retryTaskAttempt.taskAttemptId shouldBe stateIn.taskAttemptId
        retryTaskAttempt.taskAttemptRetry shouldBe stateIn.taskAttemptRetry
        retryTaskAttemptDelay shouldBe msgIn.taskAttemptDelayBeforeRetry
        taskStatusUpdated.taskId shouldBe stateIn.taskId
        taskStatusUpdated.taskName shouldBe TaskName("${stateIn.taskName}::${stateIn.methodName}")
        taskStatusUpdated.oldStatus shouldBe stateIn.taskStatus
        taskStatusUpdated.newStatus shouldBe TaskStatus.RUNNING_WARNING
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
        val test = TestEngine(stateIn, msgIn)
        val taskStateStorage = test.taskStateStorage
        val taskEventStorage = test.taskEventStorage
        val taskEngineOutput = test.taskEngineOutput
        test.handle()

        coVerify {
            taskEventStorage.insertTaskEvent(captured(taskEventStorage.taskAttemptFailedSlot)!!)
        }
        checkShouldRetryTaskAttempt(msgIn, stateIn, taskStateStorage, taskEventStorage, taskEngineOutput)
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
        val test = TestEngine(stateIn, msgIn)
        val taskStateStorage = test.taskStateStorage
        val taskEventStorage = test.taskEventStorage
        val taskEngineOutput = test.taskEngineOutput
        test.handle()

        coVerify {
            taskEventStorage.insertTaskEvent(captured(taskEventStorage.taskAttemptFailedSlot)!!)
        }
        checkShouldRetryTaskAttempt(msgIn, stateIn, taskStateStorage, taskEventStorage, taskEngineOutput)
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
        val test = TestEngine(stateIn, msgIn)
        val taskStateStorage = test.taskStateStorage
        val taskEventStorage = test.taskEventStorage
        val taskEngineOutput = test.taskEngineOutput
        test.handle()

        coVerify {
            taskEventStorage.insertTaskEvent(captured(taskEventStorage.retryTaskAttemptSlot)!!)
        }
        checkShouldRetryTaskAttempt(msgIn, stateIn, taskStateStorage, taskEventStorage, taskEngineOutput)
    }

    // TODO: add tests for retryTask with non-null parameters
})

private fun checkShouldRetryTaskAttempt(
    msgIn: TaskEngineMessage,
    stateIn: TaskState,
    taskStateStorage: MockTaskStateStorage,
    taskEventStorage: MockTaskEventStorage,
    taskEngineOutput: MockTaskEngineOutput
) {
    val runTask = captured(taskEngineOutput.workerMessageSlot)!!
    val taskAttemptDispatched = captured(taskEngineOutput.taskAttemptDispatchedSlot)!!
    val state = captured(taskStateStorage.stateSlot)!!
    val taskStatusUpdated = captured(taskEngineOutput.taskStatusUpdatedSlot)!!

    coVerifyOrder {
        taskStateStorage.getState(msgIn.taskId)
        taskEngineOutput.sendToTaskExecutors(runTask)
        taskEngineOutput.sendToTaskEngine(taskAttemptDispatched, 0F)
        taskStateStorage.updateState(msgIn.taskId, state, stateIn)
        taskEngineOutput.sendToMonitoringPerName(taskStatusUpdated)
    }
    runTask.shouldBeInstanceOf<RunTask>()
    runTask.taskId shouldBe stateIn.taskId
    runTask.taskAttemptId shouldBe stateIn.taskAttemptId
    runTask.taskAttemptRetry shouldBe stateIn.taskAttemptRetry + 1
    runTask.taskName shouldBe stateIn.taskName
    runTask.methodInput shouldBe stateIn.methodInput
    taskAttemptDispatched.taskId shouldBe stateIn.taskId
    taskAttemptDispatched.taskAttemptId shouldBe runTask.taskAttemptId
    taskAttemptDispatched.taskAttemptRetry shouldBe runTask.taskAttemptRetry
    state.taskId shouldBe stateIn.taskId
    state.taskName shouldBe stateIn.taskName
    state.methodInput shouldBe stateIn.methodInput
    state.taskAttemptId shouldBe runTask.taskAttemptId
    state.taskAttemptRetry shouldBe runTask.taskAttemptRetry
    state.taskStatus shouldBe TaskStatus.RUNNING_WARNING
    taskStatusUpdated.oldStatus shouldBe stateIn.taskStatus
    taskStatusUpdated.newStatus shouldBe TaskStatus.RUNNING_WARNING
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
