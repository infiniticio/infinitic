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
package io.infinitic.tasks.executor

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.tasks.data.TaskReturnValue
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.executors.errors.TaskFailedError
import io.infinitic.common.tasks.executors.messages.TaskCompleted
import io.infinitic.common.tasks.executors.messages.TaskFailed
import io.infinitic.common.tasks.executors.messages.TaskRetried
import io.infinitic.common.tasks.executors.messages.TaskStarted
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

private val testServiceName = ServiceName("serviceTest")
private val clientName = ClientName("testClient")
private val taskTags = setOf(TaskTag("foo"), TaskTag("bar"))
private val workflowName = WorkflowName("testWorkflow")
private val workflowId = WorkflowId()
private val methodRunId = MethodRunId()
private val testEmitterName = EmitterName("emitterTest")

class TaskEventHandlerTests :
  StringSpec(
      {
        // slots
        val afterSlot = slot<MillisDuration>()
        val taskTagSlots = CopyOnWriteArrayList<TaskTagMessage>() // multithreading update
        val clientSlot = slot<ClientMessage>()
        val workflowEngineSlot = slot<WorkflowEngineMessage>()

        // mocks
        fun completed() = CompletableFuture.completedFuture(Unit)
        val producerAsync = mockk<InfiniticProducerAsync> {
          every { name } returns "$testEmitterName"
          every { sendToTaskTagAsync(capture(taskTagSlots)) } returns completed()
          every { sendToClientAsync(capture(clientSlot)) } returns completed()
          every {
            sendToWorkflowEngineAsync(capture(workflowEngineSlot), capture(afterSlot))
          } returns completed()
        }

        val taskEventHandler = TaskEventHandler(producerAsync)

        // ensure slots are emptied between each test
        beforeTest {
          afterSlot.clear()
          taskTagSlots.clear()
          workflowEngineSlot.clear()
          clientSlot.clear()
        }

        "ExecuteTask should not be processed by the event handler" {
          val input = arrayOf(3, 3)
          val types = listOf(Int::class.java.name, Int::class.java.name)
          val msg = getExecuteTask("handle", input, types)

          coroutineScope { shouldThrow<RuntimeException> { taskEventHandler.handle(msg) } }
        }

        "on TaskStarted, should do nothing" {
          coroutineScope { taskEventHandler.handle(getTaskStarted()) }

          clientSlot.isCaptured shouldBe false
          workflowEngineSlot.isCaptured shouldBe false
          taskTagSlots.size shouldBe 0
        }

        "on TaskRetried, should do nothing" {
          coroutineScope { taskEventHandler.handle(getTaskRetried()) }

          clientSlot.isCaptured shouldBe false
          workflowEngineSlot.isCaptured shouldBe false
          taskTagSlots.size shouldBe 0
        }

        "on TaskCompleted, should send message back to parent workflow" {
          // without parent workflow
          coroutineScope { taskEventHandler.handle(getTaskCompleted()) }
          workflowEngineSlot.isCaptured shouldBe false

          // with parent workflow
          val msg = getTaskCompleted().copy(
              workflowName = workflowName,
              workflowId = workflowId,
              methodRunId = methodRunId,
          )          // when
          coroutineScope { taskEventHandler.handle(msg) }

          workflowEngineSlot.captured shouldBe getTaskCompletedWorkflow(msg)
        }

        "on TaskCompleted, should send message back to waiting client" {
          // without waiting client
          coroutineScope { taskEventHandler.handle(getTaskCompleted()) }
          clientSlot.isCaptured shouldBe false

          // with waiting client
          val msg = getTaskCompleted().copy(
              clientName = clientName,
              clientWaiting = true,
          )
          coroutineScope { taskEventHandler.handle(msg) }

          clientSlot.isCaptured shouldBe true
          clientSlot.captured shouldBe getTaskCompletedClient(msg)
        }

        "on TaskCompleted, should send messages to remove tags" {
          // without tags
          coroutineScope { taskEventHandler.handle(getTaskCompleted()) }
          taskTagSlots.size shouldBe 0

          // with tags
          val msg = getTaskCompleted().copy(taskTags = taskTags)
          coroutineScope { taskEventHandler.handle(msg) }

          taskTagSlots.size shouldBe 2
          taskTagSlots[0] shouldBe getRemoveTag(msg, "foo")
          taskTagSlots[1] shouldBe getRemoveTag(msg, "bar")
        }

        "on TaskFailed, should send message back to parent workflow" {
          // without parent workflow
          coroutineScope { taskEventHandler.handle(getTaskFailed()) }
          workflowEngineSlot.isCaptured shouldBe false

          // with parent workflow
          val msg = getTaskFailed().copy(
              workflowName = workflowName,
              workflowId = workflowId,
              methodRunId = methodRunId,
          )          // when
          coroutineScope { taskEventHandler.handle(msg) }

          workflowEngineSlot.captured shouldBe getTaskFailedWorkflow(msg)
        }

        "on TaskFailed, should send message back to waiting client" {
          // without waiting client
          coroutineScope { taskEventHandler.handle(getTaskFailed()) }
          clientSlot.isCaptured shouldBe false

          // with waiting client
          val msg = getTaskFailed().copy(
              clientName = clientName,
              clientWaiting = true,
          )
          coroutineScope { taskEventHandler.handle(msg) }

          clientSlot.isCaptured shouldBe true
          clientSlot.captured shouldBe getTaskFailedClient(msg)
        }

        "on TaskFailed, should NOT send messages to remove tags" {
          // without tags
          coroutineScope { taskEventHandler.handle(getTaskFailed()) }
          taskTagSlots.size shouldBe 0

          // with tags
          val msg = getTaskFailed().copy(taskTags = taskTags)
          coroutineScope { taskEventHandler.handle(msg) }

          taskTagSlots.size shouldBe 0
        }
      },
  )

private fun getTaskRetried() = TaskRetried(
    serviceName = testServiceName,
    taskId = TaskId(),
    emitterName = EmitterName("previousEmitter"),
    taskRetrySequence = TaskRetrySequence(12),
    taskRetryIndex = TaskRetryIndex(7),
    workflowName = null,
    workflowId = null,
    methodRunId = null,
    lastError = TestFactory.random(),
    taskMeta = TestFactory.random(),
    taskTags = TestFactory.random(),
    taskRetryDelay = TestFactory.random(),
)

private fun getTaskStarted() = TaskStarted(
    serviceName = testServiceName,
    taskId = TaskId(),
    emitterName = EmitterName("previousEmitter"),
    taskRetrySequence = TaskRetrySequence(12),
    taskRetryIndex = TaskRetryIndex(7),
    workflowName = null,
    workflowId = null,
    methodRunId = null,
    clientName = null,
    workflowVersion = WorkflowVersion(42),
    taskMeta = TestFactory.random(),
    taskTags = TestFactory.random(),
)

private fun getTaskCompleted() = TaskCompleted(
    serviceName = testServiceName,
    taskId = TaskId(),
    emitterName = EmitterName("previousEmitter"),
    taskRetrySequence = TaskRetrySequence(12),
    taskRetryIndex = TaskRetryIndex(7),
    workflowName = null,
    workflowId = null,
    methodRunId = null,
    returnValue = ReturnValue.from("42"),
    clientName = null,
    clientWaiting = null,
    workflowVersion = WorkflowVersion(42),
    taskTags = setOf(),
    taskMeta = TestFactory.random(),
)

private fun getTaskCompletedClient(msg: TaskCompleted) =
    io.infinitic.common.clients.messages.TaskCompleted(
        recipientName = msg.clientName!!,
        taskId = msg.taskId,
        taskReturnValue = msg.returnValue,
        taskMeta = msg.taskMeta,
        emitterName = testEmitterName,
    )

private fun getTaskCompletedWorkflow(msg: TaskCompleted) =
    io.infinitic.common.workflows.engine.messages.TaskCompleted(
        workflowName = msg.workflowName!!,
        workflowId = msg.workflowId!!,
        methodRunId = msg.methodRunId!!,
        taskReturnValue = TaskReturnValue(
            msg.taskId,
            msg.serviceName,
            msg.taskMeta,
            msg.returnValue,
        ),
        emitterName = testEmitterName,
    )

private fun getTaskFailed() = TaskFailed(
    serviceName = testServiceName,
    taskId = TaskId(),
    emitterName = EmitterName("previousEmitter"),
    taskRetrySequence = TaskRetrySequence(12),
    taskRetryIndex = TaskRetryIndex(7),
    workflowName = null,
    workflowId = null,
    methodRunId = null,
    clientName = null,
    clientWaiting = null,
    workflowVersion = WorkflowVersion(42),
    taskTags = TestFactory.random(),
    taskMeta = TestFactory.random(),
    deferredError = TestFactory.random(),
    executionError = TestFactory.random(),
    methodName = TestFactory.random(),
)

private fun getTaskFailedClient(msg: TaskFailed) =
    io.infinitic.common.clients.messages.TaskFailed(
        recipientName = msg.clientName!!,
        taskId = msg.taskId,
        cause = msg.executionError,
        emitterName = testEmitterName,
    )

private fun getTaskFailedWorkflow(msg: TaskFailed) =
    io.infinitic.common.workflows.engine.messages.TaskFailed(
        workflowName = msg.workflowName!!,
        workflowId = msg.workflowId!!,
        methodRunId = msg.methodRunId!!,
        emitterName = testEmitterName,
        taskFailedError = TaskFailedError(
            serviceName = msg.serviceName,
            methodName = msg.methodName,
            taskId = msg.taskId,
            cause = msg.executionError,
        ),
        deferredError = msg.deferredError,
    )
