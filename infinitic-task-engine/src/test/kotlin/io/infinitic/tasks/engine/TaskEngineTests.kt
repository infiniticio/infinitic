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

import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
import io.infinitic.common.metrics.perName.messages.TaskStatusUpdated
import io.infinitic.common.metrics.perName.transport.SendToMetricsPerName
import io.infinitic.common.tags.data.Tag
import io.infinitic.common.tags.messages.RemoveTaskTag
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tags.transport.SendToTagEngine
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
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.tasks.executors.SendToTaskExecutors
import io.infinitic.common.tasks.executors.messages.ExecuteTaskAttempt
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.infinitic.tasks.engine.storage.TaskStateStorage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.infinitic.common.clients.messages.TaskCompleted as TaskCompletedInClient
import io.infinitic.common.workflows.engine.messages.TaskCompleted as TaskCompletedInWorkflow

fun <T : Any> captured(slot: CapturingSlot<T>) = if (slot.isCaptured) slot.captured else null

lateinit var taskStateStorage: TaskStateStorage

lateinit var taskState: CapturingSlot<TaskState>
lateinit var clientMessage: CapturingSlot<ClientMessage>
lateinit var tagEngineMessage: CapturingSlot<TagEngineMessage>
lateinit var taskEngineMessage: CapturingSlot<TaskEngineMessage>
lateinit var taskEngineDelay: CapturingSlot<MillisDuration>
lateinit var workflowEngineMessage: CapturingSlot<WorkflowEngineMessage>
lateinit var taskExecutorMessage: CapturingSlot<TaskExecutorMessage>
lateinit var metricsPerNameMessage: CapturingSlot<MetricsPerNameMessage>

lateinit var sendToClient: SendToClient
lateinit var sendToTagEngine: SendToTagEngine
lateinit var sendToTaskEngine: SendToTaskEngine
lateinit var sendToWorkflowEngine: SendToWorkflowEngine
lateinit var sendToTaskExecutors: SendToTaskExecutors
lateinit var sendToMetricsPerName: SendToMetricsPerName

internal class TaskEngineTests : StringSpec({

    "CancelTask" {
        // given
        val stateIn = random<TaskState>(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_OK,
                "tags" to setOf(Tag("foo"), Tag("bar"))
            )
        )
        val msgIn = random<CancelTask>(
            mapOf(
                "taskId" to stateIn.taskId
            )
        )
        // when
        getEngine(stateIn).handle(msgIn)
        // then
        coVerifySequence {
            taskStateStorage.getState(msgIn.taskId)
            sendToWorkflowEngine(ofType<TaskCompletedInWorkflow>(), MillisDuration(0))
            sendToClient(ofType<TaskCompletedInClient>())
            sendToTaskEngine(ofType<TaskCanceled>(), MillisDuration(0))
            sendToTagEngine(ofType<RemoveTaskTag>())
            sendToTagEngine(ofType<RemoveTaskTag>())
            taskStateStorage.delState(msgIn.taskId)
            sendToMetricsPerName(ofType<TaskStatusUpdated>())
        }
        verifyAll()

        val taskCanceled = captured(taskEngineMessage)!! as TaskCanceled
        val taskStatusUpdated = captured(metricsPerNameMessage)!! as TaskStatusUpdated
        val taskCompletedInClient = captured(clientMessage)!! as TaskCompletedInClient

        with(taskCanceled) {
            taskId shouldBe msgIn.taskId
            taskMeta shouldBe stateIn.taskMeta
        }
        with(taskStatusUpdated) {
            oldStatus shouldBe stateIn.taskStatus
            newStatus shouldBe TaskStatus.TERMINATED_CANCELED
        }
        with(taskCompletedInClient) {
            taskId shouldBe stateIn.taskId
            taskReturnValue shouldBe msgIn.taskReturnValue
        }
    }

    "DispatchTask" {
        // given
        val stateIn = null
        val msgIn = random<DispatchTask>()
        // when
        getEngine(stateIn).handle(msgIn)
        // then
        coVerifySequence {
            taskStateStorage.getState(msgIn.taskId)
            sendToTaskExecutors(ofType<ExecuteTaskAttempt>())
            sendToTaskEngine(ofType<TaskAttemptDispatched>(), MillisDuration(0))
            taskStateStorage.putState(msgIn.taskId, ofType<TaskState>())
            sendToMetricsPerName(ofType<TaskStatusUpdated>())
        }
        verifyAll()

        val state = captured(taskState)!!
        val taskAttemptDispatched = captured(taskEngineMessage)!! as TaskAttemptDispatched
        val executeTaskAttempt = captured(taskExecutorMessage)!! as ExecuteTaskAttempt
        val taskStatusUpdated = captured(metricsPerNameMessage)!! as TaskStatusUpdated

        with(executeTaskAttempt) {
            taskId shouldBe msgIn.taskId
            taskName shouldBe msgIn.taskName
            methodParameters shouldBe msgIn.methodParameters
            taskAttemptRetry.int shouldBe 0
            taskAttemptId shouldBe taskAttemptDispatched.taskAttemptId
            taskAttemptRetry shouldBe taskAttemptDispatched.taskAttemptRetry
        }
        with(state) {
            taskId shouldBe msgIn.taskId
            taskName shouldBe msgIn.taskName
            methodParameters shouldBe msgIn.methodParameters
            taskAttemptId shouldBe executeTaskAttempt.taskAttemptId
            taskAttemptRetry.int shouldBe 0
            taskMeta shouldBe msgIn.taskMeta
            taskStatus shouldBe TaskStatus.RUNNING_OK
        }
        with(taskStatusUpdated) {
            oldStatus shouldBe null
            newStatus shouldBe TaskStatus.RUNNING_OK
        }
    }

    "RetryTask" {
        // given
        val stateIn = random<TaskState>(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_ERROR
            )
        )
        val msgIn = random<RetryTask>(
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
        // when
        getEngine(stateIn).handle(msgIn)
        // then
        coVerifySequence {
            taskStateStorage.getState(msgIn.taskId)
            sendToTaskExecutors(ofType<ExecuteTaskAttempt>())
            sendToTaskEngine(ofType<TaskAttemptDispatched>(), MillisDuration(0))
            taskStateStorage.putState(msgIn.taskId, ofType<TaskState>())
            sendToMetricsPerName(ofType<TaskStatusUpdated>())
        }
        verifyAll()

        val state = captured(taskState)!!
        val taskAttemptDispatched = captured(taskEngineMessage)!! as TaskAttemptDispatched
        val executeTaskAttempt = captured(taskExecutorMessage)!! as ExecuteTaskAttempt
        val taskStatusUpdated = captured(metricsPerNameMessage)!! as TaskStatusUpdated

        with(executeTaskAttempt) {
            taskId shouldBe stateIn.taskId
            taskAttemptId shouldNotBe stateIn.taskAttemptId
            taskAttemptRetry.int shouldBe 0
            taskName shouldBe stateIn.taskName
            methodParameters shouldBe stateIn.methodParameters
        }
        with(executeTaskAttempt) {
            taskId shouldBe stateIn.taskId
            taskAttemptId shouldNotBe stateIn.taskAttemptId
            taskAttemptRetry.int shouldBe 0
            taskName shouldBe stateIn.taskName
            methodParameters shouldBe stateIn.methodParameters
        }
        with(taskAttemptDispatched) {
            taskId shouldBe stateIn.taskId
            taskAttemptId shouldBe executeTaskAttempt.taskAttemptId
            taskAttemptRetry.int shouldBe 0
        }
        with(state) {
            taskId shouldBe stateIn.taskId
            taskName shouldBe stateIn.taskName
            methodParameters shouldBe stateIn.methodParameters
            taskAttemptId shouldBe executeTaskAttempt.taskAttemptId
            taskAttemptRetry shouldBe executeTaskAttempt.taskAttemptRetry
            taskStatus shouldBe TaskStatus.RUNNING_WARNING
        }
        with(taskStatusUpdated) {
            oldStatus shouldBe stateIn.taskStatus
            newStatus shouldBe TaskStatus.RUNNING_WARNING
        }
    }

    "TaskAttemptStarted" {
        // given
        val stateIn = random<TaskState>()
        val msgIn = random<TaskAttemptStarted>()
        // when
        getEngine(stateIn).handle(msgIn)
        // then
    }

    "TaskAttemptCompleted" {
        // given
        val stateIn = random<TaskState>(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_OK,
                "tags" to setOf(Tag("foo"), Tag("bar"))
            )
        )
        val msgIn = random<TaskAttemptCompleted>(
            mapOf(
                "taskId" to stateIn.taskId
            )
        )
        // when
        getEngine(stateIn).handle(msgIn)
        // then

        coVerifySequence {
            taskStateStorage.getState(msgIn.taskId)
            sendToWorkflowEngine(ofType<TaskCompletedInWorkflow>(), MillisDuration(0))
            sendToClient(ofType<TaskCompletedInClient>())
            sendToTaskEngine(ofType<TaskCompleted>(), MillisDuration(0))
            sendToTagEngine(ofType<RemoveTaskTag>())
            sendToTagEngine(ofType<RemoveTaskTag>())
            taskStateStorage.delState(msgIn.taskId)
            sendToMetricsPerName(ofType<TaskStatusUpdated>())
        }
        verifyAll()

        val taskStatusUpdated = captured(metricsPerNameMessage)!! as TaskStatusUpdated
        val taskCompleted = captured(taskEngineMessage)!! as TaskCompleted
        val taskCompletedInClient = captured(clientMessage)!! as TaskCompletedInClient

        with(taskStatusUpdated) {
            oldStatus shouldBe stateIn.taskStatus
            newStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        }
        with(taskCompleted) {
            taskReturnValue shouldBe msgIn.taskReturnValue
            taskMeta shouldBe stateIn.taskMeta
            taskId shouldBe stateIn.taskId
            taskReturnValue shouldBe msgIn.taskReturnValue
        }
        with(taskCompletedInClient) {
            taskId shouldBe stateIn.taskId
            taskReturnValue shouldBe msgIn.taskReturnValue
        }
    }

    "TaskAttemptFailed without retry" {
        // given
        val stateIn = random<TaskState>(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_OK
            )
        )
        val msgIn = random<TaskAttemptFailed>(
            mapOf(
                "taskId" to stateIn.taskId,
                "taskAttemptId" to stateIn.taskAttemptId,
                "taskAttemptRetry" to stateIn.taskAttemptRetry,
                "taskAttemptDelayBeforeRetry" to null
            )
        )
        // when
        getEngine(stateIn).handle(msgIn)
        // then
        coVerifySequence {
            taskStateStorage.getState(msgIn.taskId)
            taskStateStorage.putState(msgIn.taskId, ofType())
            sendToMetricsPerName(ofType<TaskStatusUpdated>())
        }
        verifyAll()

        val state = captured(taskState)!!
        val taskStatusUpdated = captured(metricsPerNameMessage)!! as TaskStatusUpdated

        with(taskStatusUpdated) {
            taskId shouldBe stateIn.taskId
            taskName shouldBe TaskName("${stateIn.taskName}::${stateIn.methodName}")
            oldStatus shouldBe stateIn.taskStatus
            newStatus shouldBe TaskStatus.RUNNING_ERROR
        }
    }

    "TaskAttemptFailed with future retry" {
        // given
        val stateIn = random<TaskState>(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_OK
            )
        )
        val msgIn = random<TaskAttemptFailed>(
            mapOf(
                "taskId" to stateIn.taskId,
                "taskAttemptId" to stateIn.taskAttemptId,
                "taskAttemptRetry" to stateIn.taskAttemptRetry,
                "taskAttemptDelayBeforeRetry" to MillisDuration(42000)
            )
        )
        // when
        getEngine(stateIn).handle(msgIn)
        // then
        coVerifySequence {
            taskStateStorage.getState(msgIn.taskId)
            sendToTaskEngine(ofType<RetryTaskAttempt>(), ofType<MillisDuration>())
            taskStateStorage.putState(msgIn.taskId, ofType())
            sendToMetricsPerName(ofType<TaskStatusUpdated>())
        }
        verifyAll()

        val state = captured(taskState)!!
        val taskStatusUpdated = captured(metricsPerNameMessage)!! as TaskStatusUpdated
        val retryTaskAttempt = captured(taskEngineMessage)!! as RetryTaskAttempt
        val retryTaskAttemptDelay = captured(taskEngineDelay)!!

        with(retryTaskAttempt) {
            taskId shouldBe stateIn.taskId
            taskAttemptId shouldBe stateIn.taskAttemptId
            taskAttemptRetry shouldBe stateIn.taskAttemptRetry
        }
        retryTaskAttemptDelay shouldBe msgIn.taskAttemptDelayBeforeRetry
        with(taskStatusUpdated) {
            taskId shouldBe stateIn.taskId
            taskName shouldBe TaskName("${stateIn.taskName}::${stateIn.methodName}")
            oldStatus shouldBe stateIn.taskStatus
            newStatus shouldBe TaskStatus.RUNNING_WARNING
        }
    }

    "TaskAttemptFailed with immediate retry" {
        // given
        val stateIn = random<TaskState>(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_OK
            )
        )
        val msgIn = random<TaskAttemptFailed>(
            mapOf(
                "taskId" to stateIn.taskId,
                "taskAttemptId" to stateIn.taskAttemptId,
                "taskAttemptRetry" to stateIn.taskAttemptRetry,
                "taskAttemptDelayBeforeRetry" to MillisDuration(0)
            )
        )
        // when
        getEngine(stateIn).handle(msgIn)
        // then
        checkShouldRetryTaskAttempt(msgIn, stateIn)
    }

    "TaskAttemptFailed with immediate retry (negative delay)" {
        // given
        val stateIn = random<TaskState>(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_OK
            )
        )
        val msgIn = random<TaskAttemptFailed>(
            mapOf(
                "taskId" to stateIn.taskId,
                "taskAttemptId" to stateIn.taskAttemptId,
                "taskAttemptRetry" to stateIn.taskAttemptRetry,
                "taskAttemptDelayBeforeRetry" to MillisDuration(-42000)
            )
        )
        // when
        getEngine(stateIn).handle(msgIn)
        // then
        checkShouldRetryTaskAttempt(msgIn, stateIn)
    }

    "RetryTaskAttempt" {
        // given
        val stateIn = random<TaskState>(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_ERROR
            )
        )
        val msgIn = random<RetryTaskAttempt>(
            mapOf(
                "taskId" to stateIn.taskId,
                "taskAttemptId" to stateIn.taskAttemptId,
                "taskAttemptRetry" to stateIn.taskAttemptRetry
            )
        )
        // when
        getEngine(stateIn).handle(msgIn)
        // then
        checkShouldRetryTaskAttempt(msgIn, stateIn)
    }

    // TODO: add tests for retryTask with non-null parameters
})

private fun checkShouldRetryTaskAttempt(msgIn: TaskEngineMessage, stateIn: TaskState) {
    coVerifyOrder {
        taskStateStorage.getState(msgIn.taskId)
        sendToTaskExecutors(ofType<ExecuteTaskAttempt>())
        sendToTaskEngine(ofType<TaskAttemptDispatched>(), MillisDuration(0))
        taskStateStorage.putState(msgIn.taskId, ofType())
        sendToMetricsPerName(ofType<TaskStatusUpdated>())
    }
    verifyAll()

    val state = captured(taskState)!!
    val taskStatusUpdated = captured(metricsPerNameMessage)!! as TaskStatusUpdated
    val executeTaskAttempt = captured(taskExecutorMessage)!! as ExecuteTaskAttempt
    val taskAttemptDispatched = captured(taskEngineMessage)!! as TaskAttemptDispatched

    with(executeTaskAttempt) {
        taskId shouldBe stateIn.taskId
        taskAttemptId shouldBe stateIn.taskAttemptId
        taskAttemptRetry shouldBe stateIn.taskAttemptRetry + 1
        taskName shouldBe stateIn.taskName
        methodParameters shouldBe stateIn.methodParameters
    }
    with(taskAttemptDispatched) {
        taskId shouldBe stateIn.taskId
        taskAttemptId shouldBe executeTaskAttempt.taskAttemptId
        taskAttemptRetry shouldBe executeTaskAttempt.taskAttemptRetry
    }
    with(state) {
        taskId shouldBe stateIn.taskId
        taskName shouldBe stateIn.taskName
        methodParameters shouldBe stateIn.methodParameters
        taskAttemptId shouldBe executeTaskAttempt.taskAttemptId
        taskAttemptRetry shouldBe executeTaskAttempt.taskAttemptRetry
        taskStatus shouldBe TaskStatus.RUNNING_WARNING
    }
    with(taskStatusUpdated) {
        oldStatus shouldBe stateIn.taskStatus
        newStatus shouldBe TaskStatus.RUNNING_WARNING
    }
}
private inline fun <reified T : Any> random(values: Map<String, Any?>? = null) =
    TestFactory.random<T>(values)

fun mockSendToClient(slot: CapturingSlot<ClientMessage>): SendToClient {
    val mock = mockk<SendToClient>()
    coEvery { mock(capture(slot)) } just Runs
    return mock
}

fun mockSendToTaskExecutors(slot: CapturingSlot<TaskExecutorMessage>): SendToTaskExecutors {
    val mock = mockk<SendToTaskExecutors>()
    coEvery { mock(capture(slot)) } just Runs
    return mock
}

fun mockSendToMetricsPerName(slot: CapturingSlot<MetricsPerNameMessage>): SendToMetricsPerName {
    val mock = mockk<SendToMetricsPerName>()
    coEvery { mock(capture(slot)) } returns println("mockSendToMetricsPerName")
    return mock
}

fun mockSendToTagEngine(slots: CapturingSlot<TagEngineMessage>): SendToTagEngine {
    val mock = mockk<SendToTagEngine>()
    coEvery { mock(capture(slots)) } just Runs
    return mock
}

fun mockSendToTaskEngine(slots: CapturingSlot<TaskEngineMessage>, delays: CapturingSlot<MillisDuration>): SendToTaskEngine {
    val mock = mockk<SendToTaskEngine>()
    coEvery { mock(capture(slots), capture(delays)) } just Runs
    return mock
}

fun mockSendToWorkflowEngine(slot: CapturingSlot<WorkflowEngineMessage>): SendToWorkflowEngine {
    val mock = mockk<SendToWorkflowEngine>()
    coEvery { mock(capture(slot), MillisDuration(0)) } just Runs
    return mock
}

fun mockTaskStateStorage(state: TaskState?): TaskStateStorage {
    val taskStateStorage = mockk<TaskStateStorage>()
    coEvery { taskStateStorage.getState(any()) } returns state?.deepCopy()
    coEvery { taskStateStorage.putState(any(), capture(taskState)) } just Runs
    coEvery { taskStateStorage.delState(any()) } just Runs

    return taskStateStorage
}

fun getEngine(state: TaskState?): TaskEngine {
    taskState = slot()
    taskStateStorage = mockTaskStateStorage(state)

    clientMessage = slot()
    tagEngineMessage = slot()
    taskEngineMessage = slot()
    taskEngineDelay = slot()
    workflowEngineMessage = slot()
    taskExecutorMessage = slot()
    metricsPerNameMessage = slot()

    sendToClient = mockSendToClient(clientMessage)
    sendToTagEngine = mockSendToTagEngine(tagEngineMessage)
    sendToTaskEngine = mockSendToTaskEngine(taskEngineMessage, taskEngineDelay)
    sendToWorkflowEngine = mockSendToWorkflowEngine(workflowEngineMessage)
    sendToTaskExecutors = mockSendToTaskExecutors(taskExecutorMessage)
    sendToMetricsPerName = mockSendToMetricsPerName(metricsPerNameMessage)

    return TaskEngine(
        taskStateStorage,
        sendToClient,
        sendToTagEngine,
        sendToTaskEngine,
        sendToWorkflowEngine,
        sendToTaskExecutors,
        sendToMetricsPerName
    )
}

fun verifyAll() = confirmVerified(
    sendToClient,
    sendToTagEngine,
    sendToTaskEngine,
    sendToWorkflowEngine,
    sendToTaskExecutors,
    sendToMetricsPerName
)
