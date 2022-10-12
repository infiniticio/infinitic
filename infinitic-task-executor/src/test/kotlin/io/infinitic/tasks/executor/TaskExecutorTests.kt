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

@file:Suppress("unused")

package io.infinitic.tasks.executor

import io.infinitic.clients.InfiniticClientInterface
import io.infinitic.common.clients.SendToClient
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.tasks.data.TaskReturnValue
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.executors.SendToTaskExecutorAfter
import io.infinitic.common.tasks.executors.errors.ExecutionError
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.SendToTaskTag
import io.infinitic.common.tasks.tags.messages.RemoveTagFromTask
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.workers.config.ExponentialBackoffRetry
import io.infinitic.common.workers.registry.RegisteredService
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.exceptions.tasks.ClassNotFoundException
import io.infinitic.exceptions.tasks.NoMethodFoundWithParameterCountException
import io.infinitic.exceptions.tasks.NoMethodFoundWithParameterTypesException
import io.infinitic.exceptions.tasks.TimeoutTaskException
import io.infinitic.exceptions.tasks.TooManyMethodsFoundWithParameterCountException
import io.infinitic.tasks.executor.samples.RetryImpl
import io.infinitic.tasks.executor.samples.ServiceImplService
import io.infinitic.tasks.executor.samples.ServiceWithBuggyRetryInClass
import io.infinitic.tasks.executor.samples.ServiceWithContext
import io.infinitic.tasks.executor.samples.ServiceWithRegisteredTimeout
import io.infinitic.tasks.executor.samples.ServiceWithRetryInClass
import io.infinitic.tasks.executor.samples.ServiceWithRetryInMethod
import io.infinitic.tasks.executor.samples.ServiceWithTimeoutOnClass
import io.infinitic.tasks.executor.samples.ServiceWithTimeoutOnMethod
import io.infinitic.tasks.executor.samples.SimpleServiceWithRetry
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass
import io.infinitic.common.clients.messages.TaskCompleted as TaskCompletedClient
import io.infinitic.common.clients.messages.TaskFailed as TaskFailedClient
import io.infinitic.common.workflows.engine.messages.TaskCompleted as TaskCompletedWorkflow
import io.infinitic.common.workflows.engine.messages.TaskFailed as TaskFailedWorkflow

private val clientName = ClientName("clientTaskExecutorTests")

class TaskExecutorTests : StringSpec({
    val after = slot<MillisDuration>()
    val taskExecutorMessage = slot<TaskExecutorMessage>()
    val taskTagMessages = CopyOnWriteArrayList<TaskTagMessage>() // multithreading update
    val clientMessage = slot<ClientMessage>()
    val workflowEngineMessage = slot<WorkflowEngineMessage>()
    val workerRegistry = mockk<WorkerRegistry>()
    val mockClientFactory = mockk<() -> InfiniticClientInterface>()
    val taskExecutor = TaskExecutor(
        clientName,
        workerRegistry,
        mockSendToTaskExecutor(taskExecutorMessage, after),
        mockSendToTaskTag(taskTagMessages),
        mockSendToWorkflowEngine(workflowEngineMessage),
        mockSendToClient(clientMessage),
        mockClientFactory
    )
    val service = RegisteredService(1, { ServiceImplService() }, null, null)

    // ensure slots are emptied between each test
    beforeTest {
        clearMocks(workerRegistry)
        after.clear()
        taskExecutorMessage.clear()
        taskTagMessages.clear()
        workflowEngineMessage.clear()
        clientMessage.clear()
    }

    "Task executed without waiting client neither workflow should not send message" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service
        val input = arrayOf(3, 3)
        val types = listOf(Int::class.java.name, Int::class.java.name)
        // with
        val msg = getExecuteTask("handle", input, types).copy(clientWaiting = false, workflowId = null)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        taskExecutorMessage.isCaptured shouldBe false
        clientMessage.isCaptured shouldBe false
        workflowEngineMessage.isCaptured shouldBe false
        taskTagMessages.size shouldBe 2
        taskTagMessages[0] shouldBe getRemoveTag(msg, "foo")
        taskTagMessages[1] shouldBe getRemoveTag(msg, "bar")
    }

    "Task executed with waiting client should send message to it" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service
        val input = arrayOf(3, 3)
        val types = listOf(Int::class.java.name, Int::class.java.name)
        // with
        val msg = getExecuteTask("handle", input, types).copy(workflowId = null)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        taskExecutorMessage.isCaptured shouldBe false
        clientMessage.captured shouldBe getTaskCompletedClient(msg, 9)
        workflowEngineMessage.isCaptured shouldBe false

        taskTagMessages.size shouldBe 2
        taskTagMessages[0] shouldBe getRemoveTag(msg, "foo")
        taskTagMessages[1] shouldBe getRemoveTag(msg, "bar")
    }

    "Task executed with workflow should send message to it" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service
        val input = arrayOf(3, 3)
        val types = listOf(Int::class.java.name, Int::class.java.name)
        // with
        val msg = getExecuteTask("handle", input, types).copy(clientWaiting = false)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        taskExecutorMessage.isCaptured shouldBe false
        clientMessage.isCaptured shouldBe false
        workflowEngineMessage.captured shouldBe getTaskCompletedWorkflow(msg, 9)
        taskTagMessages.size shouldBe 2
        taskTagMessages[0] shouldBe getRemoveTag(msg, "foo")
        taskTagMessages[1] shouldBe getRemoveTag(msg, "bar")
    }

    "Task executed with waiting client and workflow should send message to them" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service
        val input = arrayOf(3, 3)
        val types = listOf(Int::class.java.name, Int::class.java.name)
        // with
        val msg = getExecuteTask("handle", input, types)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        taskExecutorMessage.isCaptured shouldBe false
        clientMessage.captured shouldBe getTaskCompletedClient(msg, 9)
        workflowEngineMessage.captured shouldBe getTaskCompletedWorkflow(msg, 9)
        taskTagMessages.size shouldBe 2
        taskTagMessages[0] shouldBe getRemoveTag(msg, "foo")
        taskTagMessages[1] shouldBe getRemoveTag(msg, "bar")
    }

    "Should be able to run an explicit method with 2 parameters" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service
        val input = arrayOf(3, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getExecuteTask("other", input, types)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        taskExecutorMessage.isCaptured shouldBe false
        clientMessage.captured shouldBe getTaskCompletedClient(msg, "9")
        workflowEngineMessage.captured shouldBe getTaskCompletedWorkflow(msg, "9")
        taskTagMessages.size shouldBe 2
        taskTagMessages[0] shouldBe getRemoveTag(msg, "foo")
        taskTagMessages[1] shouldBe getRemoveTag(msg, "bar")
    }

    "Should be able to run an explicit method with 2 parameters without parameterTypes" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service
        val input = arrayOf(4, "3")
        val types = null
        val msg = getExecuteTask("other", input, types)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        taskExecutorMessage.isCaptured shouldBe false
        clientMessage.captured shouldBe getTaskCompletedClient(msg, "12")
        workflowEngineMessage.captured shouldBe getTaskCompletedWorkflow(msg, "12")
        taskTagMessages.size shouldBe 2
        taskTagMessages[0] shouldBe getRemoveTag(msg, "foo")
        taskTagMessages[1] shouldBe getRemoveTag(msg, "bar")
    }

    "Should throw ClassNotFoundException when trying to process an unknown task" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } throws ClassNotFoundException("task")
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        val msg = getExecuteTask("unknown", input, types)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        taskExecutorMessage.isCaptured shouldBe false
        checkClientException(clientMessage, msg, ClassNotFoundException::class)
        checkWorkflowException(workflowEngineMessage, msg, ClassNotFoundException::class)
        taskTagMessages.size shouldBe 2
        taskTagMessages[0] shouldBe getRemoveTag(msg, "foo")
        taskTagMessages[1] shouldBe getRemoveTag(msg, "bar")
    }

    "Should throw NoMethodFoundWithParameterTypes when trying to process an unknown method" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getExecuteTask("unknown", input, types)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        taskExecutorMessage.isCaptured shouldBe false
        checkClientException(clientMessage, msg, NoMethodFoundWithParameterTypesException::class)
        checkWorkflowException(workflowEngineMessage, msg, NoMethodFoundWithParameterTypesException::class)
        taskTagMessages.size shouldBe 2
        taskTagMessages[0] shouldBe getRemoveTag(msg, "foo")
        taskTagMessages[1] shouldBe getRemoveTag(msg, "bar")
    }

    "Should throw NoMethodFoundWithParameterCount when trying to process an unknown method without parameterTypes" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service
        val input = arrayOf(2, "3")
        // with
        val msg = getExecuteTask("unknown", input, null)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        taskExecutorMessage.isCaptured shouldBe false
        checkClientException(clientMessage, msg, NoMethodFoundWithParameterCountException::class)
        checkWorkflowException(workflowEngineMessage, msg, NoMethodFoundWithParameterCountException::class)
        taskTagMessages.size shouldBe 2
        taskTagMessages[0] shouldBe getRemoveTag(msg, "foo")
        taskTagMessages[1] shouldBe getRemoveTag(msg, "bar")
    }

    "Should throw TooManyMethodsFoundWithParameterCount when trying to process an unknown method without parameterTypes" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service
        val input = arrayOf(2, "3")
        // with
        val msg = getExecuteTask("handle", input, null)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        taskExecutorMessage.isCaptured shouldBe false
        checkClientException(clientMessage, msg, TooManyMethodsFoundWithParameterCountException::class)
        checkWorkflowException(workflowEngineMessage, msg, TooManyMethodsFoundWithParameterCountException::class)
        taskTagMessages.size shouldBe 2
        taskTagMessages[0] shouldBe getRemoveTag(msg, "foo")
        taskTagMessages[1] shouldBe getRemoveTag(msg, "bar")
    }

    "Should retry with correct exception with Retry intrface" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service.copy(factory = { SimpleServiceWithRetry() })
        // with
        val msg = getExecuteTask("handle", arrayOf(2, "3"), null)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        after.captured shouldBe MillisDuration((SimpleServiceWithRetry.DELAY * 1000).toLong())
        val executeTask = taskExecutorMessage.captured as ExecuteTask
        executeTask shouldBe msg.copy(
            taskRetryIndex = msg.taskRetryIndex + 1,
            lastError = ExecutionError.from(clientName, IllegalStateException())
                .copy(stackTraceToString = executeTask.lastError!!.stackTraceToString)
        )
        clientMessage.isCaptured shouldBe false
        workflowEngineMessage.isCaptured shouldBe false
        taskTagMessages.size shouldBe 0
    }

    "Should retry with correct exception with Retry annotation on method" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service.copy(factory = { ServiceWithRetryInMethod() })
        // with
        val msg = getExecuteTask("handle", arrayOf(2, "3"), null)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        after.captured shouldBe MillisDuration((RetryImpl.DELAY * 1000).toLong())
        val executeTask = taskExecutorMessage.captured as ExecuteTask
        executeTask shouldBe msg.copy(
            taskRetryIndex = msg.taskRetryIndex + 1,
            lastError = ExecutionError.from(clientName, IllegalStateException())
                .copy(stackTraceToString = executeTask.lastError!!.stackTraceToString)
        )
        clientMessage.isCaptured shouldBe false
        workflowEngineMessage.isCaptured shouldBe false
        taskTagMessages.size shouldBe 0
    }

    "Should retry with correct exception with Retry annotation on class" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service.copy(factory = { ServiceWithRetryInClass() })
        // with
        val msg = getExecuteTask("handle", arrayOf(2, "3"), null)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        after.captured shouldBe MillisDuration((RetryImpl.DELAY * 1000).toLong())
        val executeTask = taskExecutorMessage.captured as ExecuteTask
        executeTask shouldBe msg.copy(
            taskRetryIndex = msg.taskRetryIndex + 1,
            lastError = ExecutionError.from(clientName, IllegalStateException())
                .copy(stackTraceToString = executeTask.lastError!!.stackTraceToString)
        )
        clientMessage.isCaptured shouldBe false
        workflowEngineMessage.isCaptured shouldBe false
        taskTagMessages.size shouldBe 0
    }

    "Should throw when getSecondsBeforeRetry throw an exception" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service.copy(factory = { ServiceWithBuggyRetryInClass() })
        // with
        val msg = getExecuteTask("handle", arrayOf(2, "3"), null)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        taskExecutorMessage.isCaptured shouldBe false
        checkClientException(clientMessage, msg, IllegalArgumentException::class)
        checkWorkflowException(workflowEngineMessage, msg, IllegalArgumentException::class)
        taskTagMessages.size shouldBe 2
        taskTagMessages[0] shouldBe getRemoveTag(msg, "foo")
        taskTagMessages[1] shouldBe getRemoveTag(msg, "bar")
    }

    "Should be able to access context from task" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service.copy(factory = { ServiceWithContext() })
        val input = arrayOf(2, "3")
        // with
        val msg = getExecuteTask("handle", input, null)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        taskExecutorMessage.isCaptured shouldBe false
        clientMessage.captured shouldBe getTaskCompletedClient(msg, "72")
        workflowEngineMessage.captured shouldBe getTaskCompletedWorkflow(msg, "72")
        taskTagMessages.size shouldBe 2
        taskTagMessages[0] shouldBe getRemoveTag(msg, "foo")
        taskTagMessages[1] shouldBe getRemoveTag(msg, "bar")
    }

    "Should throw TimeoutTaskException with timeout from Registry" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service.copy(
            factory = { ServiceWithRegisteredTimeout() },
            withTimeout = { 0.1 },
            withRetry = ExponentialBackoffRetry(maximumRetries = 0)
        )
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getExecuteTask("handle", arrayOf(2, "3"), types)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        taskExecutorMessage.isCaptured shouldBe false
        checkClientException(clientMessage, msg, TimeoutTaskException::class)
        checkWorkflowException(workflowEngineMessage, msg, TimeoutTaskException::class)
        taskTagMessages.size shouldBe 2
        taskTagMessages[0] shouldBe getRemoveTag(msg, "foo")
        taskTagMessages[1] shouldBe getRemoveTag(msg, "bar")
    }

    "Should throw TimeoutTaskException with timeout from method Annotation" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service.copy(
            factory = { ServiceWithTimeoutOnMethod() },
            withRetry = ExponentialBackoffRetry(maximumRetries = 0)
        )
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getExecuteTask("handle", input, types)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        taskExecutorMessage.isCaptured shouldBe false
        checkClientException(clientMessage, msg, TimeoutTaskException::class)
        checkWorkflowException(workflowEngineMessage, msg, TimeoutTaskException::class)
        taskTagMessages.size shouldBe 2
        taskTagMessages[0] shouldBe getRemoveTag(msg, "foo")
        taskTagMessages[1] shouldBe getRemoveTag(msg, "bar")
    }

    "Should throw TimeoutTaskException with timeout from class Annotation" {
        every { workerRegistry.getRegisteredService(ServiceName("task")) } returns service.copy(
            factory = { ServiceWithTimeoutOnClass() },
            withRetry = ExponentialBackoffRetry(maximumRetries = 0)
        )
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getExecuteTask("handle", input, types)
        // when
        coroutineScope { taskExecutor.handle(msg) }
        // then
        taskExecutorMessage.isCaptured shouldBe false
        checkClientException(clientMessage, msg, TimeoutTaskException::class)
        checkWorkflowException(workflowEngineMessage, msg, TimeoutTaskException::class)
        taskTagMessages.size shouldBe 2
        taskTagMessages[0] shouldBe getRemoveTag(msg, "foo")
        taskTagMessages[1] shouldBe getRemoveTag(msg, "bar")
    }
})

private fun mockSendToTaskExecutor(
    message: CapturingSlot<TaskExecutorMessage>,
    delay: CapturingSlot<MillisDuration>
): SendToTaskExecutorAfter {
    val sendToTaskExecutorAfter = mockk<SendToTaskExecutorAfter>()
    coEvery { sendToTaskExecutorAfter(capture(message), capture(delay)) } just Runs

    return sendToTaskExecutorAfter
}

private fun mockSendToTaskTag(message: CopyOnWriteArrayList<TaskTagMessage>): SendToTaskTag {
    val sendToTaskTag = mockk<SendToTaskTag>()
    coEvery { sendToTaskTag(capture(message)) } just Runs

    return sendToTaskTag
}

private fun mockSendToClient(message: CapturingSlot<ClientMessage>): SendToClient {
    val sendToClient = mockk<SendToClient>()
    coEvery { sendToClient(capture(message)) } just Runs

    return sendToClient
}

private fun mockSendToWorkflowEngine(message: CapturingSlot<WorkflowEngineMessage>): SendToWorkflowEngine {
    val sendToWorkflowEngine = mockk<SendToWorkflowEngine>()
    coEvery { sendToWorkflowEngine(capture(message)) } just Runs

    return sendToWorkflowEngine
}

private fun getTaskCompletedClient(msg: ExecuteTask, returnValue: Any?) = TaskCompletedClient(
    recipientName = msg.emitterName,
    taskId = msg.taskId,
    taskReturnValue = ReturnValue.from(returnValue),
    taskMeta = msg.taskMeta,
    emitterName = clientName
)

private fun getTaskCompletedWorkflow(msg: ExecuteTask, returnValue: Any?) = TaskCompletedWorkflow(
    workflowName = msg.workflowName!!,
    workflowId = msg.workflowId!!,
    methodRunId = msg.methodRunId!!,
    taskReturnValue = TaskReturnValue(msg.taskId, msg.serviceName, msg.taskMeta, ReturnValue.from(returnValue)),
    emitterName = clientName
)

private fun getExecuteTask(method: String, input: Array<out Any?>, types: List<String>?) = ExecuteTask(
    clientWaiting = true,
    serviceName = ServiceName("task"),
    taskId = TaskId(),
    workflowId = WorkflowId(),
    workflowName = WorkflowName("workflowName"),
    methodRunId = MethodRunId(),
    taskRetrySequence = TaskRetrySequence(12),
    taskRetryIndex = TaskRetryIndex(7),
    lastError = null,
    methodName = MethodName(method),
    methodParameterTypes = types?.let { MethodParameterTypes(it) },
    methodParameters = MethodParameters.from(*input),
    taskTags = setOf(TaskTag("foo"), TaskTag("bar")),
    taskMeta = TaskMeta(),
    emitterName = clientName
)

private fun checkClientException(
    clientMessage: CapturingSlot<ClientMessage>,
    msg: ExecuteTask,
    exception: KClass<out Exception>
) =
    with(clientMessage.captured as TaskFailedClient) {
        recipientName shouldBe msg.emitterName
        emitterName shouldBe clientName
        taskId shouldBe msg.taskId
        with(cause) {
            workerName shouldBe clientName
            name shouldBe exception.java.name
        }
    }

private fun checkWorkflowException(
    workflowMessage: CapturingSlot<WorkflowEngineMessage>,
    msg: ExecuteTask,
    exception: KClass<out Exception>
) =
    with(workflowMessage.captured as TaskFailedWorkflow) {
        emitterName shouldBe clientName
        workflowId shouldBe msg.workflowId
        workflowName shouldBe msg.workflowName
        methodRunId shouldBe msg.methodRunId
        with(failedTaskError) {
            serviceName shouldBe msg.serviceName
            taskId shouldBe msg.taskId
            methodName shouldBe msg.methodName
            with(cause) {
                workerName shouldBe clientName
                name shouldBe exception.java.name
            }
        }
        deferredError shouldBe null
    }

private fun getRemoveTag(message: ExecuteTask, tag: String) = RemoveTagFromTask(
    taskId = message.taskId,
    serviceName = message.serviceName,
    taskTag = TaskTag(tag),
    emitterName = clientName
)
