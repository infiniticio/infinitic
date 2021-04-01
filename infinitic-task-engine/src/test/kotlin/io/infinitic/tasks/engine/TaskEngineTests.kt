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

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.monitoring.perName.messages.TaskStatusUpdated
import io.infinitic.common.tags.data.Tag
import io.infinitic.common.tags.messages.AddTaskTag
import io.infinitic.common.tags.messages.RemoveTaskTag
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.data.plus
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
import io.infinitic.common.tasks.executors.messages.ExecuteTaskAttempt
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.tasks.engine.output.TaskEngineOutput
import io.infinitic.tasks.engine.storage.states.TaskStateStorage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.infinitic.common.clients.messages.TaskCompleted as TaskCompletedInClient
import io.infinitic.common.workflows.engine.messages.TaskCompleted as TaskCompletedInWorkflow

fun <T : Any> captured(slot: CapturingSlot<T>) = if (slot.isCaptured) slot.captured else null

val stateSlot = slot<TaskState>()
val addTaskTagSlot = slot<AddTaskTag>()
val removeTaskTagSlot = slot<RemoveTaskTag>()
val taskCompletedInClient = slot<TaskCompletedInClient>()
val workerMessageSlot = slot<TaskExecutorMessage>()
val retryTaskAttemptSlot = slot<RetryTaskAttempt>()
val retryTaskAttemptDelaySlot = slot< MillisDuration >()
val taskStatusUpdatedSlot = slot<TaskStatusUpdated>()
val workflowMessageSlot = slot<WorkflowEngineMessage>()
val taskCanceledSlot = slot<TaskCanceled>()
val taskAttemptDispatchedSlot = slot<TaskAttemptDispatched>()
val taskCompletedSlot = slot<TaskCompleted>()

class TestEngine(stateIn: TaskState?, private val msgIn: TaskEngineMessage) {
    // mocking TaskStateStorage
    val taskStateStorage = mockTaskStateStorage(stateIn)
    // mocking TaskEngineOutput
    val taskEngineOutput = mockTaskEngineOutput()

    suspend fun handle() {
        val engine = TaskEngine(taskStateStorage, taskEngineOutput)

        engine.handle(msgIn)
    }
}

internal class TaskEngineTests : StringSpec({
    "CancelTask" {
        val stateIn = state(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_OK,
                "tags" to setOf(Tag("foo"), Tag("bar"))
            )
        )
        val msgIn = cancelTask(mapOf("taskId" to stateIn.taskId))
        val test = TestEngine(stateIn, msgIn)
        val taskStateStorage = test.taskStateStorage
        val taskEngineOutput = test.taskEngineOutput
        test.handle()

        val taskCanceled = captured(taskCanceledSlot)!!
        val taskStatusUpdated = captured(taskStatusUpdatedSlot)!!
        val taskCompletedInClient = captured(taskCompletedInClient)!!

        coVerifySequence {
            taskStateStorage.getState(msgIn.taskId)
            taskEngineOutput.sendToWorkflowEngine(ofType<TaskCompletedInWorkflow>())
            taskEngineOutput.sendToClientResponse(taskCompletedInClient)
            taskEngineOutput.sendToTaskEngine(taskCanceled, MillisDuration(0))
            taskEngineOutput.sendToTagEngine(ofType<RemoveTaskTag>())
            taskEngineOutput.sendToTagEngine(ofType<RemoveTaskTag>())
            taskStateStorage.delState(msgIn.taskId)
            taskEngineOutput.sendToMonitoringPerName(taskStatusUpdated)
        }
        taskCanceled.taskId shouldBe msgIn.taskId
        taskCanceled.taskMeta shouldBe stateIn.taskMeta
        taskStatusUpdated.oldStatus shouldBe stateIn.taskStatus
        taskStatusUpdated.newStatus shouldBe TaskStatus.TERMINATED_CANCELED
        taskCompletedInClient.taskId shouldBe stateIn.taskId
        taskCompletedInClient.taskReturnValue shouldBe msgIn.taskReturnValue
    }

    "DispatchTask" {
        val msgIn = dispatchTask()
        val test = TestEngine(null, msgIn)
        val taskStateStorage = test.taskStateStorage
        val taskEngineOutput = test.taskEngineOutput
        test.handle()
        val runTask = captured(workerMessageSlot)!!
        val state = captured(stateSlot)!!
        val taskStatusUpdated = captured(taskStatusUpdatedSlot)!!
        val taskAttemptDispatched = captured(taskAttemptDispatchedSlot)!!
        coVerifySequence {
            taskStateStorage.getState(msgIn.taskId)
            taskEngineOutput.sendToTaskExecutors(runTask)
            taskEngineOutput.sendToTaskEngine(taskAttemptDispatched, MillisDuration(0))
            taskStateStorage.putState(msgIn.taskId, state)
            taskEngineOutput.sendToMonitoringPerName(taskStatusUpdated)
        }
        runTask.shouldBeInstanceOf<ExecuteTaskAttempt>()
        runTask.taskId shouldBe msgIn.taskId
        runTask.taskName shouldBe msgIn.taskName
        runTask.methodParameters shouldBe msgIn.methodParameters
        runTask.taskAttemptRetry.int shouldBe 0
        runTask.taskAttemptId shouldBe taskAttemptDispatched.taskAttemptId
        runTask.taskAttemptRetry shouldBe taskAttemptDispatched.taskAttemptRetry
        state.taskId shouldBe msgIn.taskId
        state.taskName shouldBe msgIn.taskName
        state.methodParameters shouldBe msgIn.methodParameters
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
                "taskName" to stateIn.taskName,
                "methodName" to null,
                "methodParameterTypes" to null,
                "methodParameters" to null,
                "taskMeta" to null,
                "taskOptions" to null
            )
        )
        val test = TestEngine(stateIn, msgIn)
        val taskStateStorage = test.taskStateStorage
        val taskEngineOutput = test.taskEngineOutput
        test.handle()
        val executeTaskAttempt = captured(workerMessageSlot)!!
        val taskAttemptDispatched = captured(taskAttemptDispatchedSlot)!!
        val state = captured(stateSlot)!!
        val taskStatusUpdated = captured(taskStatusUpdatedSlot)!!
        coVerifySequence {
            taskStateStorage.getState(msgIn.taskId)
            taskEngineOutput.sendToTaskExecutors(executeTaskAttempt)
            taskEngineOutput.sendToTaskEngine(taskAttemptDispatched, MillisDuration(0))
            taskStateStorage.putState(msgIn.taskId, state)
            taskEngineOutput.sendToMonitoringPerName(taskStatusUpdated)
        }
        executeTaskAttempt.shouldBeInstanceOf<ExecuteTaskAttempt>()
        executeTaskAttempt.taskId shouldBe stateIn.taskId
        executeTaskAttempt.taskAttemptId shouldNotBe stateIn.taskAttemptId
        executeTaskAttempt.taskAttemptRetry.int shouldBe 0
        executeTaskAttempt.taskName shouldBe stateIn.taskName
        executeTaskAttempt.methodParameters shouldBe stateIn.methodParameters
        taskAttemptDispatched.taskId shouldBe stateIn.taskId
        taskAttemptDispatched.taskAttemptId shouldBe executeTaskAttempt.taskAttemptId
        taskAttemptDispatched.taskAttemptRetry.int shouldBe 0
        state.taskId shouldBe stateIn.taskId
        state.taskName shouldBe stateIn.taskName
        state.methodParameters shouldBe stateIn.methodParameters
        state.taskAttemptId shouldBe executeTaskAttempt.taskAttemptId
        state.taskAttemptRetry shouldBe executeTaskAttempt.taskAttemptRetry
        state.taskStatus shouldBe TaskStatus.RUNNING_WARNING
        taskStatusUpdated.oldStatus shouldBe stateIn.taskStatus
        taskStatusUpdated.newStatus shouldBe TaskStatus.RUNNING_WARNING
    }

    "TaskAttemptStarted" {
        val stateIn = state()
        val msgIn = taskAttemptStarted()
        val test = TestEngine(stateIn, msgIn)
        test.handle()
    }

    "TaskAttemptCompleted" {
        val stateIn = state(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_OK,
                "tags" to setOf(Tag("foo"), Tag("bar"))
            )
        )
        val msgIn = taskAttemptCompleted(mapOf("taskId" to stateIn.taskId))
        val test = TestEngine(stateIn, msgIn)
        val taskStateStorage = test.taskStateStorage
        val taskEngineOutput = test.taskEngineOutput
        test.handle()

        val taskStatusUpdated = captured(taskStatusUpdatedSlot)!!
        val taskCompleted = captured(taskCompletedSlot)!!
        val taskCompletedInClient = captured(taskCompletedInClient)!!
        val taskCompletedInWorkflow = captured(workflowMessageSlot)!!

        coVerifySequence {
            taskStateStorage.getState(msgIn.taskId)
            taskEngineOutput.sendToWorkflowEngine(taskCompletedInWorkflow)
            taskEngineOutput.sendToClientResponse(taskCompletedInClient)
            taskEngineOutput.sendToTaskEngine(taskCompleted, MillisDuration(0))
            taskEngineOutput.sendToTagEngine(ofType<RemoveTaskTag>())
            taskEngineOutput.sendToTagEngine(ofType<RemoveTaskTag>())
            taskStateStorage.delState(msgIn.taskId)
            taskEngineOutput.sendToMonitoringPerName(taskStatusUpdated)
        }
        taskStatusUpdated.oldStatus shouldBe stateIn.taskStatus
        taskStatusUpdated.newStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        taskCompleted.taskReturnValue shouldBe msgIn.taskReturnValue
        taskCompleted.taskMeta shouldBe stateIn.taskMeta
        taskCompleted.taskId shouldBe stateIn.taskId
        taskCompleted.taskReturnValue shouldBe msgIn.taskReturnValue
        taskCompletedInClient.taskId shouldBe stateIn.taskId
        taskCompletedInClient.taskReturnValue shouldBe msgIn.taskReturnValue
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
        val taskEngineOutput = test.taskEngineOutput
        test.handle()

        val state = captured(stateSlot)!!
        val taskStatusUpdated = captured(taskStatusUpdatedSlot)!!
        coVerifySequence {
            taskStateStorage.getState(msgIn.taskId)
            taskStateStorage.putState(msgIn.taskId, state)
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
                "taskAttemptDelayBeforeRetry" to MillisDuration(42000)
            )
        )
        val test = TestEngine(stateIn, msgIn)
        val taskStateStorage = test.taskStateStorage
        val taskEngineOutput = test.taskEngineOutput
        test.handle()

        val state = captured(stateSlot)!!
        val taskStatusUpdated = captured(taskStatusUpdatedSlot)!!
        val retryTaskAttempt = captured(retryTaskAttemptSlot)!!
        val retryTaskAttemptDelay = captured(retryTaskAttemptDelaySlot)!!

        coVerifySequence {
            taskStateStorage.getState(msgIn.taskId)
            taskEngineOutput.sendToTaskEngine(retryTaskAttempt, retryTaskAttemptDelay)
            taskStateStorage.putState(msgIn.taskId, state)
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
                "taskAttemptDelayBeforeRetry" to MillisDuration(0)
            )
        )
        val test = TestEngine(stateIn, msgIn)
        val taskStateStorage = test.taskStateStorage
        val taskEngineOutput = test.taskEngineOutput
        test.handle()

        checkShouldRetryTaskAttempt(msgIn, stateIn, taskStateStorage, taskEngineOutput)
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
                "taskAttemptDelayBeforeRetry" to MillisDuration(-42000)
            )
        )
        val test = TestEngine(stateIn, msgIn)
        val taskStateStorage = test.taskStateStorage
        val taskEngineOutput = test.taskEngineOutput
        test.handle()

        checkShouldRetryTaskAttempt(msgIn, stateIn, taskStateStorage, taskEngineOutput)
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
        val taskEngineOutput = test.taskEngineOutput
        test.handle()

        checkShouldRetryTaskAttempt(msgIn, stateIn, taskStateStorage, taskEngineOutput)
    }

    // TODO: add tests for retryTask with non-null parameters
})

private fun checkShouldRetryTaskAttempt(
    msgIn: TaskEngineMessage,
    stateIn: TaskState,
    taskStateStorage: TaskStateStorage,
    taskEngineOutput: TaskEngineOutput
) {
    val runTask = captured(workerMessageSlot)!!
    val taskAttemptDispatched = captured(taskAttemptDispatchedSlot)!!
    val state = captured(stateSlot)!!
    val taskStatusUpdated = captured(taskStatusUpdatedSlot)!!

    coVerifyOrder {
        taskStateStorage.getState(msgIn.taskId)
        taskEngineOutput.sendToTaskExecutors(runTask)
        taskEngineOutput.sendToTaskEngine(taskAttemptDispatched, MillisDuration(0))
        taskStateStorage.putState(msgIn.taskId, state)
        taskEngineOutput.sendToMonitoringPerName(taskStatusUpdated)
    }
    runTask.shouldBeInstanceOf<ExecuteTaskAttempt>()
    runTask.taskId shouldBe stateIn.taskId
    runTask.taskAttemptId shouldBe stateIn.taskAttemptId
    runTask.taskAttemptRetry shouldBe stateIn.taskAttemptRetry + 1
    runTask.taskName shouldBe stateIn.taskName
    runTask.methodParameters shouldBe stateIn.methodParameters
    taskAttemptDispatched.taskId shouldBe stateIn.taskId
    taskAttemptDispatched.taskAttemptId shouldBe runTask.taskAttemptId
    taskAttemptDispatched.taskAttemptRetry shouldBe runTask.taskAttemptRetry
    state.taskId shouldBe stateIn.taskId
    state.taskName shouldBe stateIn.taskName
    state.methodParameters shouldBe stateIn.methodParameters
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

fun mockTaskEngineOutput(): TaskEngineOutput {
    val mock = mockk<TaskEngineOutput>()

    coEvery { mock.sendToClientResponse(capture(taskCompletedInClient)) } just Runs
    coEvery { mock.sendToTaskExecutors(capture(workerMessageSlot)) } just Runs
    coEvery { mock.sendToMonitoringPerName(capture(taskStatusUpdatedSlot)) } just Runs
    coEvery { mock.sendToTagEngine(capture(addTaskTagSlot)) } just Runs
    coEvery { mock.sendToTagEngine(capture(removeTaskTagSlot)) } just Runs
    coEvery { mock.sendToTaskEngine(capture(retryTaskAttemptSlot), capture(retryTaskAttemptDelaySlot)) } just Runs
    coEvery { mock.sendToTaskEngine(capture(taskCanceledSlot), MillisDuration(0)) } just Runs
    coEvery { mock.sendToTaskEngine(capture(taskAttemptDispatchedSlot), MillisDuration(0)) } just Runs
    coEvery { mock.sendToTaskEngine(capture(taskCompletedSlot), MillisDuration(0)) } just Runs
    coEvery { mock.sendToWorkflowEngine(capture(workflowMessageSlot)) } just Runs

    return mock
}

fun mockTaskStateStorage(state: TaskState?): TaskStateStorage {
    val taskStateStorage = mockk<TaskStateStorage>()
    coEvery { taskStateStorage.getState(any()) } returns state?.deepCopy()
    coEvery { taskStateStorage.putState(any(), capture(stateSlot)) } just Runs
    coEvery { taskStateStorage.delState(any()) } just Runs

    return taskStateStorage
}
