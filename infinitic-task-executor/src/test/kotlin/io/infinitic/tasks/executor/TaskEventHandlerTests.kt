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
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.tasks.data.TaskReturnValue
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.events.messages.TaskFailedEvent
import io.infinitic.common.tasks.events.messages.TaskRetriedEvent
import io.infinitic.common.tasks.events.messages.TaskStartedEvent
import io.infinitic.common.tasks.executors.errors.TaskFailedError
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.DelayedWorkflowEngineTopic
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.ServiceTagTopic
import io.infinitic.common.transport.WorkflowEngineTopic
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
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
private val methodRunId = WorkflowMethodId()
private val testEmitterName = EmitterName("emitterTest")

private val emittedAt = MillisInstant.now()

class TaskEventHandlerTests :
  StringSpec(
      {
        // slots
        val afterSlot = slot<MillisDuration>()
        val taskTagSlots = CopyOnWriteArrayList<ServiceTagMessage>() // multithreading update
        val clientSlot = slot<ClientMessage>()
        val workflowEngineSlot = slot<WorkflowEngineMessage>()

        // mocks
        fun completed() = CompletableFuture.completedFuture(Unit)
        val producerAsync = mockk<InfiniticProducerAsync> {
          every { producerName } returns "$testEmitterName"
          coEvery { capture(taskTagSlots).sendToAsync(ServiceTagTopic) } returns completed()
          coEvery { capture(clientSlot).sendToAsync(ClientTopic) } returns completed()
          coEvery {
            capture(workflowEngineSlot).sendToAsync(WorkflowEngineTopic)
          } returns completed()
          coEvery {
            capture(workflowEngineSlot).sendToAsync(
                DelayedWorkflowEngineTopic,
                capture(afterSlot),
            )
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

        "on TaskStarted, should do nothing" {
          coroutineScope { taskEventHandler.handle(getTaskStarted(), emittedAt) }

          clientSlot.isCaptured shouldBe false
          workflowEngineSlot.isCaptured shouldBe false
          taskTagSlots.size shouldBe 0
        }

        "on TaskRetried, should do nothing" {
          coroutineScope { taskEventHandler.handle(getTaskRetried(), emittedAt) }

          clientSlot.isCaptured shouldBe false
          workflowEngineSlot.isCaptured shouldBe false
          taskTagSlots.size shouldBe 0
        }

        "on TaskCompleted, should send message back to parent workflow" {
          // without parent workflow
          coroutineScope { taskEventHandler.handle(getTaskCompleted(), emittedAt) }
          workflowEngineSlot.isCaptured shouldBe false

          // with parent workflow
          val msg = getTaskCompleted().copy(
              workflowName = workflowName,
              workflowId = workflowId,
              workflowMethodId = methodRunId,
          )          // when
          coroutineScope { taskEventHandler.handle(msg, emittedAt) }

          workflowEngineSlot.captured shouldBe getTaskCompletedWorkflow(msg)
        }

        "on TaskCompleted, should send message back to waiting client" {
          // without waiting client
          coroutineScope { taskEventHandler.handle(getTaskCompleted(), emittedAt) }
          clientSlot.isCaptured shouldBe false

          // with waiting client
          val msg = getTaskCompleted().copy(
              clientName = clientName,
              clientWaiting = true,
          )
          coroutineScope { taskEventHandler.handle(msg, emittedAt) }

          clientSlot.isCaptured shouldBe true
          clientSlot.captured shouldBe getTaskCompletedClient(msg)
        }

        "on TaskCompleted, should send messages to remove tags" {
          // without tags
          coroutineScope { taskEventHandler.handle(getTaskCompleted(), emittedAt) }
          taskTagSlots.size shouldBe 0

          // with tags
          val msg = getTaskCompleted().copy(taskTags = taskTags)
          coroutineScope { taskEventHandler.handle(msg, emittedAt) }

          taskTagSlots.size shouldBe 2
          setOf(taskTagSlots[0], taskTagSlots[1]) shouldBe setOf(
              getRemoveTag(msg, "foo"),
              getRemoveTag(msg, "bar"),
          )
        }

        "on TaskFailed, should send message back to parent workflow" {
          // without parent workflow
          coroutineScope { taskEventHandler.handle(getTaskFailed(), emittedAt) }
          workflowEngineSlot.isCaptured shouldBe false

          // with parent workflow
          val msg = getTaskFailed().copy(
              workflowName = workflowName,
              workflowId = workflowId,
              workflowMethodId = methodRunId,
          )          // when
          coroutineScope { taskEventHandler.handle(msg, emittedAt) }

          workflowEngineSlot.captured shouldBe getTaskFailedWorkflow(msg)
        }

        "on TaskFailed, should send message back to waiting client" {
          // without waiting client
          coroutineScope { taskEventHandler.handle(getTaskFailed(), emittedAt) }
          clientSlot.isCaptured shouldBe false

          // with waiting client
          val msg = getTaskFailed().copy(
              clientName = clientName,
              clientWaiting = true,
          )
          coroutineScope { taskEventHandler.handle(msg, emittedAt) }

          clientSlot.isCaptured shouldBe true
          clientSlot.captured shouldBe getTaskFailedClient(msg)
        }

        "on TaskFailed, should NOT send messages to remove tags" {
          // without tags
          coroutineScope { taskEventHandler.handle(getTaskFailed(), emittedAt) }
          taskTagSlots.size shouldBe 0

          // with tags
          val msg = getTaskFailed().copy(taskTags = taskTags)
          coroutineScope { taskEventHandler.handle(msg, emittedAt) }

          taskTagSlots.size shouldBe 0
        }
      },
  )

private fun getTaskRetried() = TaskRetriedEvent(
    serviceName = testServiceName,
    taskId = TaskId(),
    emitterName = EmitterName("previousEmitter"),
    taskRetrySequence = TaskRetrySequence(12),
    taskRetryIndex = TaskRetryIndex(7),
    workflowName = null,
    workflowId = null,
    workflowMethodId = null,
    clientName = null,
    clientWaiting = null,
    lastError = TestFactory.random(),
    taskMeta = TestFactory.random(),
    taskTags = TestFactory.random(),
    taskRetryDelay = TestFactory.random(),
)

private fun getTaskStarted() = TaskStartedEvent(
    serviceName = testServiceName,
    taskId = TaskId(),
    emitterName = EmitterName("previousEmitter"),
    taskRetrySequence = TaskRetrySequence(12),
    taskRetryIndex = TaskRetryIndex(7),
    workflowName = null,
    workflowId = null,
    workflowMethodId = null,
    clientName = null,
    clientWaiting = null,
    taskTags = TestFactory.random(),
    taskMeta = TestFactory.random(),
    workflowVersion = WorkflowVersion(42),
)

private fun getTaskCompleted() = TaskCompletedEvent(
    serviceName = testServiceName,
    taskId = TaskId(),
    emitterName = EmitterName("previousEmitter"),
    taskRetrySequence = TaskRetrySequence(12),
    taskRetryIndex = TaskRetryIndex(7),
    workflowName = null,
    workflowId = null,
    workflowMethodId = null,
    clientName = null,
    clientWaiting = null,
    taskTags = setOf(),
    taskMeta = TestFactory.random(),
    returnValue = ReturnValue.from("42"),
    workflowVersion = WorkflowVersion(42),
)

private fun getTaskCompletedClient(msg: TaskCompletedEvent) =
    io.infinitic.common.clients.messages.TaskCompleted(
        recipientName = msg.clientName!!,
        taskId = msg.taskId,
        taskReturnValue = msg.returnValue,
        taskMeta = msg.taskMeta,
        emitterName = testEmitterName,
    )

private fun getTaskCompletedWorkflow(msg: TaskCompletedEvent) =
    io.infinitic.common.workflows.engine.messages.TaskCompleted(
        workflowName = msg.workflowName!!,
        workflowId = msg.workflowId!!,
        workflowMethodId = msg.workflowMethodId!!,
        taskReturnValue = TaskReturnValue(
            msg.taskId,
            msg.serviceName,
            msg.taskMeta,
            msg.returnValue,
        ),
        emitterName = testEmitterName,
        emittedAt = emittedAt,
    )

private fun getTaskFailed() = TaskFailedEvent(
    serviceName = testServiceName,
    taskId = TaskId(),
    emitterName = EmitterName("previousEmitter"),
    taskRetrySequence = TaskRetrySequence(12),
    taskRetryIndex = TaskRetryIndex(7),
    workflowName = null,
    workflowId = null,
    workflowMethodId = null,
    clientName = null,
    clientWaiting = null,
    taskTags = TestFactory.random(),
    taskMeta = TestFactory.random(),
    executionError = TestFactory.random(),
    deferredError = TestFactory.random(),
    methodName = TestFactory.random(),
    workflowVersion = WorkflowVersion(42),
)

private fun getTaskFailedClient(msg: TaskFailedEvent) =
    io.infinitic.common.clients.messages.TaskFailed(
        recipientName = msg.clientName!!,
        taskId = msg.taskId,
        cause = msg.executionError,
        emitterName = testEmitterName,
    )

private fun getTaskFailedWorkflow(msg: TaskFailedEvent) =
    io.infinitic.common.workflows.engine.messages.TaskFailed(
        workflowName = msg.workflowName!!,
        workflowId = msg.workflowId!!,
        workflowMethodId = msg.workflowMethodId!!,
        taskFailedError = TaskFailedError(
            serviceName = msg.serviceName,
            methodName = msg.methodName,
            taskId = msg.taskId,
            cause = msg.executionError,
        ),
        deferredError = msg.deferredError,
        emitterName = testEmitterName,
        emittedAt = emittedAt,
    )
