/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
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
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.events.messages.TaskFailedEvent
import io.infinitic.common.tasks.events.messages.TaskRetriedEvent
import io.infinitic.common.tasks.events.messages.TaskStartedEvent
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.executors.messages.clientName
import io.infinitic.common.tasks.tags.messages.RemoveTagFromTask
import io.infinitic.common.transport.DelayedServiceExecutorTopic
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.ServiceEventsTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.workers.config.ExponentialBackoffRetryPolicy
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workers.data.WorkerName
import io.infinitic.common.workers.registry.RegisteredServiceExecutor
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.exceptions.tasks.ClassNotFoundException
import io.infinitic.exceptions.tasks.NoMethodFoundWithParameterCountException
import io.infinitic.exceptions.tasks.NoMethodFoundWithParameterTypesException
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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeoutException

private val testServiceName = ServiceName("serviceTest")
private val testEmitterName = EmitterName("emitterTest")
private val testWorkerName = WorkerName.from(testEmitterName)

class TaskExecutorTests :
  StringSpec(
      {
        // slots
        val afterSlot = slot<MillisDuration>()
        val taskExecutorSlot = slot<ServiceExecutorMessage>()
        val taskEventSlot = CopyOnWriteArrayList<ServiceEventMessage>()

        // mocks
        fun completed() = CompletableFuture.completedFuture(Unit)
        val workerRegistry = mockk<WorkerRegistry>()
        val client = mockk<InfiniticClientInterface>()
        val producerAsync = mockk<InfiniticProducerAsync> {
          every { producerName } returns "$testWorkerName"
          coEvery {
            capture(taskExecutorSlot).sendToAsync(ServiceExecutorTopic)
          } returns completed()
          coEvery {
            capture(taskExecutorSlot).sendToAsync(DelayedServiceExecutorTopic, capture(afterSlot))
          } returns completed()
          coEvery { capture(taskEventSlot).sendToAsync(ServiceEventsTopic) } returns completed()
        }

        var taskExecutor = TaskExecutor(workerRegistry, producerAsync, client)

        val service = RegisteredServiceExecutor(1, { ServiceImplService() }, null, null)

        // ensure slots are emptied between each test
        beforeTest {
          clearMocks(workerRegistry)
          afterSlot.clear()
          taskExecutorSlot.clear()
          taskEventSlot.clear()
        }

        "Task executed should send TaskStarted and TaskCompleted events" {
          every { workerRegistry.getRegisteredService(testServiceName) } returns service
          val input = arrayOf(3, 3)
          val types = listOf(Int::class.java.name, Int::class.java.name)
          // with
          val msg = getExecuteTask("handle", input, types).copy(
              clientWaiting = false,
              workflowId = null,
          )
          // when
          taskExecutor.handle(msg, MillisInstant.now())
          // then
          taskExecutorSlot.isCaptured shouldBe false
          taskEventSlot.size shouldBe 2
          taskEventSlot[0] shouldBe getTaskStarted(msg, taskEventSlot[0]!!.messageId!!)
          taskEventSlot[1] shouldBe getTaskCompleted(
              msg,
              taskEventSlot[1]!!.messageId!!,
              ServiceImplService().handle(3, 3),
              msg.taskMeta,
          )
        }

        "Should be able to run an explicit method with 2 parameters" {
          every { workerRegistry.getRegisteredService(testServiceName) } returns service
          val input = arrayOf(3, "3")
          val types = listOf(Int::class.java.name, String::class.java.name)
          // with
          val msg = getExecuteTask("other", input, types)
          // when
          taskExecutor.handle(msg, MillisInstant.now())
          // then
          taskExecutorSlot.isCaptured shouldBe false
          taskEventSlot.size shouldBe 2
          taskEventSlot[0] shouldBe getTaskStarted(msg, taskEventSlot[0].messageId!!)
          taskEventSlot[1] shouldBe getTaskCompleted(
              msg,
              taskEventSlot[1]!!.messageId!!,
              ServiceImplService().other(3, "3"),
              msg.taskMeta,
          )
        }

        "Should be able to run an explicit method with 2 parameters without parameterTypes" {
          every { workerRegistry.getRegisteredService(testServiceName) } returns service
          val input = arrayOf(4, "3")
          val types = null
          val msg = getExecuteTask("other", input, types)
          // when
          taskExecutor.handle(msg, MillisInstant.now())
          // then
          taskExecutorSlot.isCaptured shouldBe false
          taskEventSlot.size shouldBe 2
          taskEventSlot[0] shouldBe getTaskStarted(msg, taskEventSlot[0].messageId!!)
          taskEventSlot[1] shouldBe getTaskCompleted(
              msg,
              taskEventSlot[1]!!.messageId!!,
              ServiceImplService().other(4, "3"),
              msg.taskMeta,
          )
        }

        "Throwable should not be caught on task" {
          every { workerRegistry.getRegisteredService(testServiceName) } returns service
          val input = arrayOf<Any>()
          val types = listOf<String>()
          val msg = getExecuteTask("withThrowable", input, types)
          // when
          val throwable = shouldThrow<Throwable> {
            taskExecutor.handle(msg, MillisInstant.now())
          }
          // then
          taskExecutorSlot.isCaptured shouldBe false
          taskEventSlot.size shouldBe 1
          taskEventSlot[0] shouldBe getTaskStarted(msg, taskEventSlot[0].messageId!!)
          throwable.message shouldBe "test throwable"

          // Note that the Throwable sent (and not caught) during the test has the side effect
          // to cancel the coroutineScope of producerAsync, that's why we recreate it after the test
          // in a real case, the Throwable would kill the worker
          taskExecutor = TaskExecutor(workerRegistry, producerAsync, client)
        }

        "Should throw ClassNotFoundException when trying to process an unknown task" {
          every { workerRegistry.getRegisteredService(testServiceName) } throws
              ClassNotFoundException("task")
          val input = arrayOf(2, "3")
          val types = listOf(Int::class.java.name, String::class.java.name)
          val msg = getExecuteTask("unknown", input, types)
          // when
          taskExecutor.handle(msg, MillisInstant.now())
          // then
          taskExecutorSlot.isCaptured shouldBe false
          taskEventSlot.size shouldBe 2
          taskEventSlot[0] shouldBe getTaskStarted(msg, taskEventSlot[0].messageId!!)
          taskEventSlot[1].shouldBeInstanceOf<TaskFailedEvent>()
          (taskEventSlot[1] as TaskFailedEvent).check(
              msg,
              ClassNotFoundException::class.java.name,
              msg.taskMeta,
          )
        }

        "Should throw NoMethodFoundWithParameterTypesException when trying to process an unknown method" {
          every { workerRegistry.getRegisteredService(testServiceName) } returns service
          val input = arrayOf(2, "3")
          val types = listOf(Int::class.java.name, String::class.java.name)
          // with
          val msg = getExecuteTask("unknown", input, types)
          // when
          taskExecutor.handle(msg, MillisInstant.now())
          // then
          taskExecutorSlot.isCaptured shouldBe false
          taskEventSlot.size shouldBe 2
          taskEventSlot[0] shouldBe getTaskStarted(msg, taskEventSlot[0].messageId!!)
          taskEventSlot[1].shouldBeInstanceOf<TaskFailedEvent>()
          (taskEventSlot[1] as TaskFailedEvent).check(
              msg,
              NoMethodFoundWithParameterTypesException::class.java.name,
              msg.taskMeta,
          )
        }

        "Should throw NoMethodFoundWithParameterCount when trying to process an unknown method without parameterTypes" {
          every { workerRegistry.getRegisteredService(testServiceName) } returns service
          val input = arrayOf(2, "3")
          // with
          val msg = getExecuteTask("unknown", input, null)
          // when
          taskExecutor.handle(msg, MillisInstant.now())
          // then
          taskExecutorSlot.isCaptured shouldBe false
          taskEventSlot.size shouldBe 2
          taskEventSlot[0] shouldBe getTaskStarted(msg, taskEventSlot[0].messageId!!)
          taskEventSlot[1].shouldBeInstanceOf<TaskFailedEvent>()
          (taskEventSlot[1] as TaskFailedEvent).check(
              msg,
              NoMethodFoundWithParameterCountException::class.java.name,
              msg.taskMeta,
          )
        }

        "Should throw TooManyMethodsFoundWithParameterCount when trying to process an unknown method without parameterTypes" {
          every { workerRegistry.getRegisteredService(testServiceName) } returns service
          val input = arrayOf(2, "3")
          // with
          val msg = getExecuteTask("handle", input, null)
          // when
          taskExecutor.handle(msg, MillisInstant.now())
          // then
          taskExecutorSlot.isCaptured shouldBe false
          taskEventSlot.size shouldBe 2
          taskEventSlot[0] shouldBe getTaskStarted(msg, taskEventSlot[0].messageId!!)
          taskEventSlot[1].shouldBeInstanceOf<TaskFailedEvent>()
          (taskEventSlot[1] as TaskFailedEvent).check(
              msg,
              TooManyMethodsFoundWithParameterCountException::class.java.name,
              msg.taskMeta,
          )
        }

        "Should retry with correct exception with Retry interface" {
          every { workerRegistry.getRegisteredService(testServiceName) } returns
              service.copy(factory = { SimpleServiceWithRetry() })
          // with
          val msg = getExecuteTask("handle", arrayOf(2, "3"), null)
          // when
          taskExecutor.handle(msg, MillisInstant.now())
          // then
          afterSlot.captured shouldBe MillisDuration((SimpleServiceWithRetry.DELAY * 1000).toLong())
          (taskExecutorSlot.captured).shouldBeInstanceOf<ExecuteTask>()
          val executeTask = taskExecutorSlot.captured as ExecuteTask
          executeTask.check(msg, IllegalStateException::class.java.name, msg.taskMeta.map)

          taskEventSlot.size shouldBe 2
          taskEventSlot[0] shouldBe getTaskStarted(msg, taskEventSlot[0].messageId!!)
          taskEventSlot[1].shouldBeInstanceOf<TaskRetriedEvent>()
          (taskEventSlot[1] as TaskRetriedEvent).check(
              msg,
              afterSlot.captured,
              IllegalStateException::class.java.name,
              msg.taskMeta,
          )
        }

        "Should retry with correct exception with Retry annotation on method" {
          every { workerRegistry.getRegisteredService(testServiceName) } returns
              service.copy(factory = { ServiceWithRetryInMethod() })
          // with
          val msg = getExecuteTask("handle", arrayOf(2, "3"), null)
          // when
          taskExecutor.handle(msg, MillisInstant.now())
          // then
          afterSlot.captured shouldBe MillisDuration((RetryImpl.DELAY * 1000).toLong())

          (taskExecutorSlot.captured).shouldBeInstanceOf<ExecuteTask>()
          val executeTask = taskExecutorSlot.captured as ExecuteTask
          executeTask.check(msg, IllegalStateException::class.java.name, msg.taskMeta.map)

          taskEventSlot.size shouldBe 2
          taskEventSlot[0] shouldBe getTaskStarted(msg, taskEventSlot[0].messageId!!)
          taskEventSlot[1].shouldBeInstanceOf<TaskRetriedEvent>()
          (taskEventSlot[1] as TaskRetriedEvent).check(
              msg,
              afterSlot.captured,
              IllegalStateException::class.java.name,
              msg.taskMeta.map,
          )
        }

        "Should retry with correct exception with Retry annotation on class" {
          every { workerRegistry.getRegisteredService(testServiceName) } returns
              service.copy(factory = { ServiceWithRetryInClass() })
          // with
          val msg = getExecuteTask("handle", arrayOf(2, "3"), null)
          // when
          taskExecutor.handle(msg, MillisInstant.now())
          // then
          afterSlot.captured shouldBe MillisDuration((RetryImpl.DELAY * 1000).toLong())

          (taskExecutorSlot.captured).shouldBeInstanceOf<ExecuteTask>()
          val executeTask = taskExecutorSlot.captured as ExecuteTask
          executeTask.check(msg, IllegalStateException::class.java.name, msg.taskMeta.map)

          taskEventSlot.size shouldBe 2
          taskEventSlot[0] shouldBe getTaskStarted(msg, taskEventSlot[0].messageId!!)
          taskEventSlot[1].shouldBeInstanceOf<TaskRetriedEvent>()
          (taskEventSlot[1] as TaskRetriedEvent).check(
              msg,
              afterSlot.captured,
              IllegalStateException::class.java.name,
              msg.taskMeta.map,
          )
        }

        "Should not retry if error in getSecondsBeforeRetry" {
          every { workerRegistry.getRegisteredService(testServiceName) } returns service.copy(
              factory = { ServiceWithBuggyRetryInClass() },
          )
          // with
          val msg = getExecuteTask("handle", arrayOf(2, "3"), null)
          // when
          taskExecutor.handle(msg, MillisInstant.now())
          // then
          taskExecutorSlot.isCaptured shouldBe false
          taskEventSlot.size shouldBe 2
          taskEventSlot[0] shouldBe getTaskStarted(msg, taskEventSlot[0].messageId!!)
          taskEventSlot[1].shouldBeInstanceOf<TaskFailedEvent>()
          (taskEventSlot[1] as TaskFailedEvent).check(
              msg,
              IllegalStateException::class.java.name,
              msg.taskMeta.map,
          )
        }

        "Should be able to access context from task" {
          every { workerRegistry.getRegisteredService(testServiceName) } returns
              service.copy(factory = { ServiceWithContext() })
          val input = arrayOf(2, "3")
          // with
          val msg = getExecuteTask("handle", input, null)
          // when
          taskExecutor.handle(msg, MillisInstant.now())
          // then
          taskExecutorSlot.isCaptured shouldBe false
          taskEventSlot.size shouldBe 2
          taskEventSlot[0] shouldBe getTaskStarted(msg, taskEventSlot[0].messageId!!)
          taskEventSlot[1] shouldBe getTaskCompleted(
              msg,
              taskEventSlot[1].messageId!!,
              (6 * msg.taskRetrySequence.toInt()).toString(),
              msg.taskMeta.map,
          )
        }

        "Should throw TimeoutException with timeout from Registry" {
          every { workerRegistry.getRegisteredService(testServiceName) } returns
              service.copy(
                  factory = { ServiceWithRegisteredTimeout() },
                  withTimeout = { 0.1 },
                  withRetry = ExponentialBackoffRetryPolicy(maximumRetries = 0),
              )
          val types = listOf(Int::class.java.name, String::class.java.name)
          // with
          val msg = getExecuteTask("handle", arrayOf(2, "3"), types)
          // when
          taskExecutor.handle(msg, MillisInstant.now())
          // then
          taskExecutorSlot.isCaptured shouldBe false
          taskEventSlot.size shouldBe 2
          taskEventSlot[0] shouldBe getTaskStarted(msg, taskEventSlot[0].messageId!!)
          taskEventSlot[1].shouldBeInstanceOf<TaskFailedEvent>()
          (taskEventSlot[1] as TaskFailedEvent).check(
              msg,
              TimeoutException::class.java.name,
              msg.taskMeta.map,
          )
        }

        "Should throw TimeoutException with timeout from method Annotation" {
          every { workerRegistry.getRegisteredService(testServiceName) } returns
              service.copy(
                  factory = { ServiceWithTimeoutOnMethod() },
                  withRetry = ExponentialBackoffRetryPolicy(maximumRetries = 0),
              )
          val input = arrayOf(2, "3")
          val types = listOf(Int::class.java.name, String::class.java.name)
          // with
          val msg = getExecuteTask("handle", input, types)
          // when
          taskExecutor.handle(msg, MillisInstant.now())
          // then
          taskExecutorSlot.isCaptured shouldBe false
          taskEventSlot.size shouldBe 2
          taskEventSlot[0] shouldBe getTaskStarted(msg, taskEventSlot[0].messageId!!)
          taskEventSlot[1].shouldBeInstanceOf<TaskFailedEvent>()
          (taskEventSlot[1] as TaskFailedEvent).check(
              msg,
              TimeoutException::class.java.name,
              msg.taskMeta,
          )
        }

        "Should throw TimeoutException with timeout from class Annotation" {
          every { workerRegistry.getRegisteredService(testServiceName) } returns
              service.copy(
                  factory = { ServiceWithTimeoutOnClass() },
                  withRetry = ExponentialBackoffRetryPolicy(maximumRetries = 0),
              )
          val input = arrayOf(2, "3")
          val types = listOf(Int::class.java.name, String::class.java.name)
          // with
          val msg = getExecuteTask("handle", input, types)
          // when
          taskExecutor.handle(msg, MillisInstant.now())
          // then
          taskExecutorSlot.isCaptured shouldBe false
          taskEventSlot.size shouldBe 2
          taskEventSlot[0] shouldBe getTaskStarted(msg, taskEventSlot[0].messageId!!)
          taskEventSlot[1].shouldBeInstanceOf<TaskFailedEvent>()
          (taskEventSlot[1] as TaskFailedEvent).check(
              msg,
              TimeoutException::class.java.name,
              msg.taskMeta,
          )
        }
      },
  )

fun ExecuteTask.check(
  msg: ExecuteTask,
  errorName: String,
  meta: MutableMap<String, ByteArray>
) {
  serviceName shouldBe msg.serviceName
  taskId shouldBe msg.taskId
  emitterName shouldBe testEmitterName
  taskRetrySequence shouldBe msg.taskRetrySequence
  taskRetryIndex shouldBe msg.taskRetryIndex + 1
  workflowName shouldBe msg.workflowName
  workflowId shouldBe msg.workflowId
  workflowVersion shouldBe msg.workflowVersion
  workflowMethodId shouldBe msg.workflowMethodId
  clientName shouldBe msg.clientName
  clientWaiting shouldBe msg.clientWaiting
  methodName shouldBe msg.methodName
  taskMeta shouldBe TaskMeta(meta)
  taskId shouldBe msg.taskId
  lastError!!.name shouldBe errorName
  lastError!!.workerName shouldBe WorkerName.from(msg.emitterName)
}

fun TaskFailedEvent.check(
  msg: ExecuteTask,
  errorName: String,
  meta: MutableMap<String, ByteArray>
) {
  serviceName shouldBe msg.serviceName
  taskId shouldBe msg.taskId
  emitterName shouldBe testEmitterName
  taskRetrySequence shouldBe msg.taskRetrySequence
  taskRetryIndex shouldBe msg.taskRetryIndex
  workflowName shouldBe msg.workflowName
  workflowId shouldBe msg.workflowId
  workflowVersion shouldBe msg.workflowVersion
  workflowMethodId shouldBe msg.workflowMethodId
  clientName shouldBe msg.clientName
  clientWaiting shouldBe msg.clientWaiting
  executionError.name shouldBe errorName
  executionError.workerName shouldBe WorkerName.from(msg.emitterName)
  methodName shouldBe msg.methodName
  taskMeta shouldBe TaskMeta(meta)
  taskId shouldBe msg.taskId
}

fun TaskRetriedEvent.check(
  msg: ExecuteTask,
  delay: MillisDuration,
  errorName: String,
  meta: MutableMap<String, ByteArray>
) {
  serviceName shouldBe msg.serviceName
  taskId shouldBe msg.taskId
  emitterName shouldBe testEmitterName
  taskRetrySequence shouldBe msg.taskRetrySequence
  taskRetryIndex shouldBe msg.taskRetryIndex + 1
  workflowName shouldBe msg.workflowName
  workflowId shouldBe msg.workflowId
  workflowMethodId shouldBe msg.workflowMethodId
  taskTags shouldBe msg.taskTags
  taskMeta shouldBe TaskMeta(meta)
  taskRetryDelay shouldBe delay
  lastError.name shouldBe errorName
  lastError.workerName shouldBe WorkerName.from(msg.emitterName)
}

internal fun getExecuteTask(method: String, input: Array<out Any?>, types: List<String>?) =
    ExecuteTask(
        serviceName = testServiceName,
        taskId = TaskId(),
        emitterName = testEmitterName,
        taskRetrySequence = TaskRetrySequence(12),
        taskRetryIndex = TaskRetryIndex(7),
        workflowName = null,
        workflowId = null,
        workflowMethodId = null,
        taskTags = TestFactory.random(),
        taskMeta = TestFactory.random(),
        clientWaiting = false,
        methodName = MethodName(method),
        methodParameterTypes = types?.let { MethodParameterTypes(it) },
        methodParameters = MethodParameters.from(*input),
        lastError = null,
        workflowVersion = WorkflowVersion(42),
    )

private fun getTaskStarted(msg: ExecuteTask, messageId: MessageId) = TaskStartedEvent(
    messageId = messageId,
    serviceName = msg.serviceName,
    taskId = msg.taskId,
    emitterName = testEmitterName,
    taskRetrySequence = msg.taskRetrySequence,
    taskRetryIndex = msg.taskRetryIndex,
    workflowName = msg.workflowName,
    workflowId = msg.workflowId,
    workflowMethodId = msg.workflowMethodId,
    clientName = msg.clientName,
    clientWaiting = msg.clientWaiting,
    taskTags = msg.taskTags,
    taskMeta = msg.taskMeta,
    workflowVersion = msg.workflowVersion,
)

private fun getTaskCompleted(
  msg: ExecuteTask,
  messageId: MessageId,
  value: Any?,
  meta: MutableMap<String, ByteArray>
) = TaskCompletedEvent(
    messageId = messageId,
    serviceName = msg.serviceName,
    taskId = msg.taskId,
    emitterName = testEmitterName,
    taskRetrySequence = msg.taskRetrySequence,
    taskRetryIndex = msg.taskRetryIndex,
    workflowName = msg.workflowName,
    workflowId = msg.workflowId,
    workflowMethodId = msg.workflowMethodId,
    clientName = msg.clientName,
    clientWaiting = msg.clientWaiting,
    taskTags = msg.taskTags,
    taskMeta = TaskMeta(meta),
    returnValue = ReturnValue.from(value),
    workflowVersion = msg.workflowVersion,
)

internal fun getRemoveTag(message: ServiceEventMessage, tag: String) = RemoveTagFromTask(
    taskId = message.taskId,
    serviceName = message.serviceName,
    taskTag = TaskTag(tag),
    emitterName = testEmitterName,
)
