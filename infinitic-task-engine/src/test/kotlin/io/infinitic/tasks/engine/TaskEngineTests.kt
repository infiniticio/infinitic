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

import io.infinitic.common.clients.SendToClient
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.data.plus
import io.infinitic.common.tasks.engines.SendToTaskEngine
import io.infinitic.common.tasks.engines.SendToTaskEngineAfter
import io.infinitic.common.tasks.engines.messages.CancelTask
import io.infinitic.common.tasks.engines.messages.DispatchTask
import io.infinitic.common.tasks.engines.messages.RetryTask
import io.infinitic.common.tasks.engines.messages.RetryTaskAttempt
import io.infinitic.common.tasks.engines.messages.TaskAttemptCompleted
import io.infinitic.common.tasks.engines.messages.TaskAttemptFailed
import io.infinitic.common.tasks.engines.messages.TaskEngineMessage
import io.infinitic.common.tasks.engines.state.TaskState
import io.infinitic.common.tasks.engines.storage.TaskStateStorage
import io.infinitic.common.tasks.executors.SendToTaskExecutor
import io.infinitic.common.tasks.executors.messages.ExecuteTaskAttempt
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.SendToTaskTag
import io.infinitic.common.tasks.tags.messages.RemoveTagFromTask
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.TaskFailed
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
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
import io.infinitic.common.clients.messages.TaskCanceled as TaskCanceledInClient
import io.infinitic.common.clients.messages.TaskCompleted as TaskCompletedInClient
import io.infinitic.common.clients.messages.TaskFailed as TaskFailedInClient
import io.infinitic.common.workflows.engine.messages.TaskCanceled as TaskCanceledInWorkflow
import io.infinitic.common.workflows.engine.messages.TaskCompleted as TaskCompletedInWorkflow

private fun <T : Any> captured(slot: CapturingSlot<T>) = if (slot.isCaptured) slot.captured else null

private val clientName = ClientName("clientTaskEngineTests")

private lateinit var taskStateStorage: TaskStateStorage

private lateinit var taskState: CapturingSlot<TaskState>
private lateinit var clientMessage: CapturingSlot<ClientMessage>
private lateinit var tagEngineMessage: CapturingSlot<TaskTagMessage>
private lateinit var taskEngineMessage: CapturingSlot<TaskEngineMessage>
private lateinit var taskEngineDelay: CapturingSlot<MillisDuration>
private lateinit var workflowEngineMessage: CapturingSlot<WorkflowEngineMessage>
private lateinit var taskExecutorMessage: CapturingSlot<TaskExecutorMessage>

private lateinit var sendToClient: SendToClient
private lateinit var sendToTaskTagEngine: SendToTaskTag
private lateinit var sendToTaskEngine: SendToTaskEngine
private lateinit var sendToTaskEngineAfter: SendToTaskEngineAfter
private lateinit var sendToWorkflowEngine: SendToWorkflowEngine
private lateinit var sendToTaskExecutors: SendToTaskExecutor

internal class TaskEngineTests : StringSpec({

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
            taskStateStorage.putState(msgIn.taskId, ofType())
        }
        verifyAll()

        val state = captured(taskState)!!
        val executeTaskAttempt = captured(taskExecutorMessage)!! as ExecuteTaskAttempt

        with(executeTaskAttempt) {
            taskId shouldBe msgIn.taskId
            taskName shouldBe msgIn.taskName
            methodParameters shouldBe msgIn.methodParameters
            taskRetryIndex.int shouldBe 0
            taskRetryIndex shouldBe TaskRetryIndex(0)
        }
        with(state) {
            taskId shouldBe msgIn.taskId
            taskName shouldBe msgIn.taskName
            methodParameters shouldBe msgIn.methodParameters
            taskAttemptId shouldBe executeTaskAttempt.taskAttemptId
            taskRetryIndex.int shouldBe 0
            taskMeta shouldBe msgIn.taskMeta
            taskStatus shouldBe TaskStatus.RUNNING_OK
        }
    }

    "CancelTask" {
        // given
        val stateIn = random<TaskState>(
            mapOf(
                "taskStatus" to TaskStatus.RUNNING_OK,
                "taskTags" to setOf(TaskTag("foo"), TaskTag("bar")),
                "waitingClients" to mutableSetOf(ClientName("foo"))
            )
        )
        val msgIn = random<CancelTask>(mapOf("taskId" to stateIn.taskId.toString()))
        // when
        getEngine(stateIn).handle(msgIn)
        // then
        coVerifySequence {
            taskStateStorage.getState(msgIn.taskId)
            sendToWorkflowEngine(ofType<TaskCanceledInWorkflow>())
            sendToClient(ofType<TaskCanceledInClient>())
            sendToTaskTagEngine(ofType<RemoveTagFromTask>())
            sendToTaskTagEngine(ofType<RemoveTagFromTask>())
            taskStateStorage.delState(msgIn.taskId)
        }
        verifyAll()

        val taskCanceledInClient = captured(clientMessage)!! as TaskCanceledInClient

        with(taskCanceledInClient) {
            taskId shouldBe stateIn.taskId
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
                "taskId" to stateIn.taskId.toString(),
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
            taskStateStorage.putState(msgIn.taskId, ofType<TaskState>())
        }
        verifyAll()

        val state = captured(taskState)!!
        val executeTaskAttempt = captured(taskExecutorMessage)!! as ExecuteTaskAttempt

        with(executeTaskAttempt) {
            taskId shouldBe stateIn.taskId
            taskAttemptId shouldNotBe stateIn.taskAttemptId
            taskRetryIndex.int shouldBe 0
            taskName shouldBe stateIn.taskName
            methodParameters shouldBe stateIn.methodParameters
        }
        with(executeTaskAttempt) {
            taskId shouldBe stateIn.taskId
            taskAttemptId shouldNotBe stateIn.taskAttemptId
            taskRetryIndex.int shouldBe 0
            taskName shouldBe stateIn.taskName
            methodParameters shouldBe stateIn.methodParameters
        }
        with(state) {
            taskId shouldBe stateIn.taskId
            taskName shouldBe stateIn.taskName
            methodParameters shouldBe stateIn.methodParameters
            taskAttemptId shouldBe executeTaskAttempt.taskAttemptId
            taskRetryIndex shouldBe executeTaskAttempt.taskRetryIndex
            taskStatus shouldBe TaskStatus.RUNNING_OK
        }
    }

    "TaskAttemptCompleted" {
        // given
        val stateIn = random<TaskState>(
            mapOf(
                "waitingClients" to mutableSetOf(ClientName("foo"), ClientName("bar")),
                "taskStatus" to TaskStatus.RUNNING_OK,
                "taskTags" to setOf(TaskTag("foo"), TaskTag("bar"))
            )
        )
        val msgIn = random<TaskAttemptCompleted>(
            mapOf(
                "taskId" to stateIn.taskId.toString()
            )
        )
        // when
        getEngine(stateIn).handle(msgIn)
        // then

        coVerifySequence {
            taskStateStorage.getState(msgIn.taskId)
            sendToWorkflowEngine(ofType<TaskCompletedInWorkflow>())
            sendToClient(ofType<TaskCompletedInClient>())
            sendToClient(ofType<TaskCompletedInClient>())
            sendToTaskTagEngine(ofType<RemoveTagFromTask>())
            sendToTaskTagEngine(ofType<RemoveTagFromTask>())
            taskStateStorage.delState(msgIn.taskId)
        }
        verifyAll()

        val taskCompletedInClient = captured(clientMessage)!! as TaskCompletedInClient

        with(taskCompletedInClient) {
            taskId shouldBe stateIn.taskId
            taskReturnValue shouldBe msgIn.taskReturnValue
        }
    }

    "TaskAttemptFailed without retry" {
        // given
        val stateIn = random<TaskState>(
            mapOf(
                "waitingClients" to mutableSetOf(ClientName("foo"), ClientName("bar")),
                "taskStatus" to TaskStatus.RUNNING_OK
            )
        )
        val msgIn = random<TaskAttemptFailed>(
            mapOf(
                "taskId" to stateIn.taskId.toString(),
                "taskAttemptId" to stateIn.taskAttemptId.toString(),
                "taskRetryIndex" to stateIn.taskRetryIndex,
                "taskAttemptDelayBeforeRetry" to null
            )
        )
        // when
        getEngine(stateIn).handle(msgIn)
        // then
        coVerifySequence {
            taskStateStorage.getState(msgIn.taskId)
            sendToWorkflowEngine(ofType<TaskFailed>())
            sendToClient(ofType<TaskFailedInClient>())
            sendToClient(ofType<TaskFailedInClient>())
            taskStateStorage.putState(msgIn.taskId, ofType())
        }
        verifyAll()
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
                "taskId" to stateIn.taskId.toString(),
                "taskAttemptId" to stateIn.taskAttemptId.toString(),
                "taskRetryIndex" to stateIn.taskRetryIndex,
                "taskAttemptDelayBeforeRetry" to MillisDuration(42000)
            )
        )
        // when
        getEngine(stateIn).handle(msgIn)
        // then
        coVerifySequence {
            taskStateStorage.getState(msgIn.taskId)
            sendToTaskEngineAfter(ofType<RetryTaskAttempt>(), ofType())
            taskStateStorage.putState(msgIn.taskId, ofType())
        }
        verifyAll()

        val retryTaskAttempt = captured(taskEngineMessage)!! as RetryTaskAttempt
        val retryTaskAttemptDelay = captured(taskEngineDelay)!!

        with(retryTaskAttempt) {
            taskId shouldBe stateIn.taskId
            taskAttemptId shouldBe stateIn.taskAttemptId
            taskRetryIndex shouldBe stateIn.taskRetryIndex
        }
        retryTaskAttemptDelay shouldBe msgIn.taskAttemptDelayBeforeRetry
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
                "taskId" to stateIn.taskId.toString(),
                "taskAttemptId" to stateIn.taskAttemptId.toString(),
                "taskRetryIndex" to stateIn.taskRetryIndex,
                "taskAttemptDelayBeforeRetry" to MillisDuration.ZERO
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
                "taskId" to stateIn.taskId.toString(),
                "taskAttemptId" to stateIn.taskAttemptId.toString(),
                "taskRetryIndex" to stateIn.taskRetryIndex,
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
                "taskId" to stateIn.taskId.toString(),
                "taskAttemptId" to stateIn.taskAttemptId.toString(),
                "taskRetryIndex" to stateIn.taskRetryIndex
            )
        )
        // when
        getEngine(stateIn).handle(msgIn)
        // then
        checkShouldRetryTaskAttempt(msgIn, stateIn)
    }
})

private fun checkShouldRetryTaskAttempt(msgIn: TaskEngineMessage, stateIn: TaskState) {
    coVerifyOrder {
        taskStateStorage.getState(msgIn.taskId)
        sendToTaskExecutors(ofType<ExecuteTaskAttempt>())
        taskStateStorage.putState(msgIn.taskId, ofType())
    }
    verifyAll()

    val state = captured(taskState)!!
    val executeTaskAttempt = captured(taskExecutorMessage)!! as ExecuteTaskAttempt

    with(executeTaskAttempt) {
        taskId shouldBe stateIn.taskId
        taskAttemptId shouldBe stateIn.taskAttemptId
        taskRetryIndex shouldBe stateIn.taskRetryIndex + 1
        taskName shouldBe stateIn.taskName
        methodParameters shouldBe stateIn.methodParameters
    }
    with(state) {
        taskId shouldBe stateIn.taskId
        taskName shouldBe stateIn.taskName
        methodParameters shouldBe stateIn.methodParameters
        taskAttemptId shouldBe executeTaskAttempt.taskAttemptId
        taskRetryIndex shouldBe executeTaskAttempt.taskRetryIndex
        taskStatus shouldBe TaskStatus.RUNNING_WARNING
    }
}

private inline fun <reified T : Any> random(values: Map<String, Any?>? = null) =
    TestFactory.random<T>(values)

private fun mockSendToClient(slot: CapturingSlot<ClientMessage>): SendToClient {
    val mock = mockk<SendToClient>()
    coEvery { mock(capture(slot)) } just Runs
    return mock
}

private fun mockSendToTaskExecutors(slot: CapturingSlot<TaskExecutorMessage>): SendToTaskExecutor {
    val mock = mockk<SendToTaskExecutor>()
    coEvery { mock(capture(slot)) } just Runs
    return mock
}

private fun mockSendToTagEngine(slots: CapturingSlot<TaskTagMessage>): SendToTaskTag {
    val mock = mockk<SendToTaskTag>()
    coEvery { mock(capture(slots)) } just Runs
    return mock
}

private fun mockSendToTaskEngine(slots: CapturingSlot<TaskEngineMessage>): SendToTaskEngine {
    val mock = mockk<SendToTaskEngine>()
    coEvery { mock(capture(slots)) } just Runs
    return mock
}

private fun mockSendToTaskEngineAfter(slots: CapturingSlot<TaskEngineMessage>, delays: CapturingSlot<MillisDuration>): SendToTaskEngineAfter {
    val mock = mockk<SendToTaskEngineAfter>()
    coEvery { mock(capture(slots), capture(delays)) } just Runs
    return mock
}

private fun mockSendToWorkflowEngine(slot: CapturingSlot<WorkflowEngineMessage>): SendToWorkflowEngine {
    val mock = mockk<SendToWorkflowEngine>()
    coEvery { mock(capture(slot)) } just Runs
    return mock
}

private fun mockTaskStateStorage(state: TaskState?): TaskStateStorage {
    val taskStateStorage = mockk<TaskStateStorage>()
    coEvery { taskStateStorage.getState(TaskId(any())) } returns state?.deepCopy()
    coEvery { taskStateStorage.putState(TaskId(any()), capture(taskState)) } just Runs
    coEvery { taskStateStorage.delState(TaskId(any())) } just Runs

    return taskStateStorage
}

private fun getEngine(state: TaskState?): TaskEngine {
    taskState = slot()
    taskStateStorage = mockTaskStateStorage(state)

    clientMessage = slot()
    tagEngineMessage = slot()
    taskEngineMessage = slot()
    taskEngineDelay = slot()
    workflowEngineMessage = slot()
    taskExecutorMessage = slot()

    sendToClient = mockSendToClient(clientMessage)
    sendToTaskTagEngine = mockSendToTagEngine(tagEngineMessage)
    sendToTaskEngine = mockSendToTaskEngine(taskEngineMessage)
    sendToTaskEngineAfter = mockSendToTaskEngineAfter(taskEngineMessage, taskEngineDelay)
    sendToWorkflowEngine = mockSendToWorkflowEngine(workflowEngineMessage)
    sendToTaskExecutors = mockSendToTaskExecutors(taskExecutorMessage)

    return TaskEngine(
        clientName,
        taskStateStorage,
        sendToClient,
        sendToTaskTagEngine,
        sendToTaskEngineAfter,
        sendToWorkflowEngine,
        sendToTaskExecutors
    )
}

private fun verifyAll() = confirmVerified(
    sendToClient,
    sendToTaskTagEngine,
    sendToTaskEngine,
    sendToWorkflowEngine,
    sendToTaskExecutors
)
