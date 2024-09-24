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
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.requester.ClientRequester
import io.infinitic.common.requester.Requester
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.requester.clientName
import io.infinitic.common.requester.workflowId
import io.infinitic.common.requester.workflowMethodId
import io.infinitic.common.requester.workflowMethodName
import io.infinitic.common.requester.workflowName
import io.infinitic.common.requester.workflowVersion
import io.infinitic.common.tasks.data.DelegatedTaskData
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
import io.infinitic.common.tasks.tags.messages.RemoveTaskIdFromTag
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.tasks.tags.messages.SetDelegatedTaskData
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.transport.ServiceTagEngineTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowStateTimerTopic
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.RemoteTaskCompleted
import io.infinitic.common.workflows.engine.messages.RemoteTaskFailed
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

private val testServiceName = ServiceName("serviceTest")
private val clientName = ClientName("testClient")
private val taskTags = setOf(TaskTag("foo"), TaskTag("bar"))
private val workflowName = WorkflowName("testWorkflow")
private val workflowId = WorkflowId()
private val methodRunId = WorkflowMethodId()
private val testEmitterName = EmitterName("emitterTest")
private val clientRequester = ClientRequester(clientName = clientName)
private val workflowRequester = WorkflowRequester(
    workflowId = workflowId,
    workflowName = workflowName,
    workflowVersion = WorkflowVersion(2),
    workflowMethodId = methodRunId,
    workflowMethodName = MethodName("methodTest"),
)
private val emittedAt = MillisInstant.now()

class TaskEventHandlerTests :
  StringSpec(
      {
        // slots
        val afterSlot = slot<MillisDuration>()
        val taskTagSlots = CopyOnWriteArrayList<ServiceTagMessage>() // multithreading update
        val clientSlot = slot<ClientMessage>()
        val workflowEngineSlot = slot<WorkflowStateEngineMessage>()

        // mocks
        fun completed() = CompletableFuture.completedFuture(Unit)
        val producerAsync = mockk<InfiniticProducer> {
          coEvery { getName() } returns "$testEmitterName"
          coEvery { capture(taskTagSlots).sendTo(ServiceTagEngineTopic) } returns Unit
          coEvery { capture(clientSlot).sendTo(ClientTopic) } returns Unit
          coEvery {
            capture(workflowEngineSlot).sendTo(WorkflowStateEngineTopic)
          } returns Unit
          coEvery {
            capture(workflowEngineSlot).sendTo(
                WorkflowStateTimerTopic,
                capture(afterSlot),
            )
          } returns Unit
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
          taskEventHandler.handle(getTaskStarted(workflowRequester), emittedAt)

          clientSlot.isCaptured shouldBe false
          workflowEngineSlot.isCaptured shouldBe false
          taskTagSlots.size shouldBe 0
        }

        "on TaskRetried, should do nothing" {
          taskEventHandler.handle(getTaskRetried(workflowRequester), emittedAt)

          clientSlot.isCaptured shouldBe false
          workflowEngineSlot.isCaptured shouldBe false
          taskTagSlots.size shouldBe 0
        }

        "on TaskCompleted, should send message back to parent workflow" {
          // requested by client
          taskEventHandler.handle(getTaskCompleted(clientRequester), emittedAt)
          clientSlot.isCaptured shouldBe false
          workflowEngineSlot.isCaptured shouldBe false

          // requested by workflow, but async
          taskEventHandler.handle(
              getTaskCompleted(workflowRequester).copy(isDelegated = true),
              emittedAt,
          )
          workflowEngineSlot.isCaptured shouldBe false

          // with parent workflow
          val msg = getTaskCompleted(workflowRequester)
          taskEventHandler.handle(msg, emittedAt)
          workflowEngineSlot.captured shouldBe getTaskCompletedWorkflow(msg)
        }

        "on TaskCompleted, should send message back to waiting client" {
          // requested by workflow
          taskEventHandler.handle(getTaskCompleted(workflowRequester), emittedAt)
          clientSlot.isCaptured shouldBe false

          // requested by client, but not waiting
          taskEventHandler.handle(getTaskCompleted(clientRequester), emittedAt)
          clientSlot.isCaptured shouldBe false

          // requested by waiting client, but async
          taskEventHandler.handle(
              getTaskCompleted(clientRequester).copy(clientWaiting = true, isDelegated = true),
              emittedAt,
          )
          clientSlot.isCaptured shouldBe false

          // sync task, requested by waiting client
          val msg = getTaskCompleted(clientRequester).copy(clientWaiting = true)
          taskEventHandler.handle(msg, emittedAt)

          clientSlot.isCaptured shouldBe true
          clientSlot.captured shouldBe getTaskCompletedClient(msg)
        }

        "on TaskCompleted, should send messages to remove tags" {
          // without tags
          taskEventHandler.handle(getTaskCompleted(clientRequester), emittedAt)
          taskTagSlots.size shouldBe 0

          // with tags, but async
          taskEventHandler.handle(
              getTaskCompleted(clientRequester).copy(taskTags = taskTags, isDelegated = true),
              emittedAt,
          )
          taskTagSlots.size shouldBe 1
          taskTagSlots.clear()

          // with tags
          val msg = getTaskCompleted(clientRequester).copy(taskTags = taskTags)
          taskEventHandler.handle(msg, emittedAt)

          taskTagSlots.size shouldBe 2
          val msg0 = taskTagSlots[0] as RemoveTaskIdFromTag
          val msg1 = taskTagSlots[1] as RemoveTaskIdFromTag

          val msgFoo = listOf(msg0, msg1).first { it.taskTag == TaskTag("foo") }
          val msgBar = listOf(msg0, msg1).first { it.taskTag == TaskTag("bar") }

          msgFoo shouldBe getRemoveTag(msg, "foo").copy(messageId = msgFoo.messageId)
          msgBar shouldBe getRemoveTag(msg, "bar").copy(messageId = msgBar.messageId)
        }

        "on TaskCompleted, if asynchronous, should send SetDelegatedTaskData to tag engine" {
          val msg =
              getTaskCompleted(workflowRequester).copy(taskTags = taskTags, isDelegated = true)
          taskEventHandler.handle(msg, emittedAt)

          workflowEngineSlot.isCaptured shouldBe false
          clientSlot.isCaptured shouldBe false
          taskTagSlots.size shouldBe 1

          taskTagSlots[0] shouldBe SetDelegatedTaskData(
              delegatedTaskData = DelegatedTaskData(
                  serviceName = msg.serviceName,
                  methodName = msg.methodName,
                  taskId = msg.taskId,
                  requester = msg.requester,
                  clientWaiting = msg.clientWaiting,
                  taskMeta = msg.taskMeta,
              ),
              messageId = taskTagSlots[0].messageId!!,
              serviceName = msg.serviceName,
              taskId = msg.taskId,
              emitterName = testEmitterName,
          )

        }

        "on TaskFailed, should send message back to parent workflow" {
          // without parent workflow
          taskEventHandler.handle(getTaskFailed(clientRequester), emittedAt)
          workflowEngineSlot.isCaptured shouldBe false

          // with parent workflow
          val msg = getTaskFailed(workflowRequester)
          taskEventHandler.handle(msg, emittedAt)
          workflowEngineSlot.captured shouldBe getTaskFailedWorkflow(msg)
        }

        "on TaskFailed, should send message back to waiting client" {
          // without waiting client
          taskEventHandler.handle(getTaskFailed(workflowRequester), emittedAt)
          clientSlot.isCaptured shouldBe false

          // with waiting client
          val msg = getTaskFailed(clientRequester).copy(clientWaiting = true)
          taskEventHandler.handle(msg, emittedAt)
          clientSlot.isCaptured shouldBe true
          clientSlot.captured shouldBe getTaskFailedClient(msg)
        }

        "on TaskFailed, should NOT send messages to remove tags" {
          // without tags
          taskEventHandler.handle(getTaskFailed(clientRequester), emittedAt)
          taskTagSlots.size shouldBe 0

          // with tags
          val msg = getTaskFailed(clientRequester).copy(taskTags = taskTags)
          taskEventHandler.handle(msg, emittedAt)

          taskTagSlots.size shouldBe 0
        }
      },
  )

private fun getTaskRetried(requester: Requester) = TaskRetriedEvent(
    serviceName = testServiceName,
    taskId = TaskId(),
    methodName = TestFactory.random(),
    emitterName = EmitterName("previousEmitter"),
    taskRetrySequence = TaskRetrySequence(12),
    taskRetryIndex = TaskRetryIndex(7),
    requester = requester,
    clientWaiting = null,
    lastError = TestFactory.random(),
    taskMeta = TestFactory.random(),
    taskTags = TestFactory.random(),
    taskRetryDelay = TestFactory.random(),
)

private fun getTaskStarted(requester: Requester) = TaskStartedEvent(
    serviceName = testServiceName,
    taskId = TaskId(),
    methodName = TestFactory.random(),
    emitterName = EmitterName("previousEmitter"),
    taskRetrySequence = TaskRetrySequence(12),
    taskRetryIndex = TaskRetryIndex(7),
    requester = requester,
    clientWaiting = null,
    taskTags = TestFactory.random(),
    taskMeta = TestFactory.random(),
)

private fun getTaskCompleted(requester: Requester) = TaskCompletedEvent(
    serviceName = testServiceName,
    taskId = TaskId(),
    methodName = TestFactory.random(),
    emitterName = EmitterName("previousEmitter"),
    taskRetrySequence = TaskRetrySequence(12),
    taskRetryIndex = TaskRetryIndex(7),
    requester = requester,
    clientWaiting = null,
    taskTags = setOf(),
    taskMeta = TestFactory.random(),
    returnValue = MethodReturnValue.from("42", null),
    isDelegated = false,
)

private fun getTaskCompletedClient(msg: TaskCompletedEvent) =
    io.infinitic.common.clients.messages.TaskCompleted(
        recipientName = msg.requester.clientName!!,
        taskId = msg.taskId,
        returnValue = msg.returnValue,
        taskMeta = msg.taskMeta,
        emitterName = testEmitterName,
    )

private fun getTaskCompletedWorkflow(msg: TaskCompletedEvent) =
    RemoteTaskCompleted(
        workflowName = msg.requester.workflowName!!,
        workflowVersion = msg.requester.workflowVersion,
        workflowId = msg.requester.workflowId!!,
        workflowMethodName = msg.requester.workflowMethodName!!,
        workflowMethodId = msg.requester.workflowMethodId!!,
        taskReturnValue = TaskReturnValue(
            msg.taskId,
            msg.serviceName,
            msg.methodName,
            msg.taskMeta,
            msg.returnValue,
        ),
        emitterName = testEmitterName,
        emittedAt = emittedAt,
    )

private fun getTaskFailed(requester: Requester) = TaskFailedEvent(
    serviceName = testServiceName,
    methodName = TestFactory.random(),
    taskId = TaskId(),
    emitterName = EmitterName("previousEmitter"),
    taskRetrySequence = TaskRetrySequence(12),
    taskRetryIndex = TaskRetryIndex(7),
    requester = requester,
    clientWaiting = null,
    taskTags = TestFactory.random(),
    taskMeta = TestFactory.random(),
    executionError = TestFactory.random(),
    deferredError = TestFactory.random(),
)

private fun getTaskFailedClient(msg: TaskFailedEvent) =
    io.infinitic.common.clients.messages.TaskFailed(
        recipientName = msg.requester.clientName!!,
        taskId = msg.taskId,
        cause = msg.executionError,
        emitterName = testEmitterName,
    )

private fun getTaskFailedWorkflow(msg: TaskFailedEvent) =
    RemoteTaskFailed(
        workflowName = msg.requester.workflowName!!,
        workflowVersion = msg.requester.workflowVersion,
        workflowId = msg.requester.workflowId!!,
        workflowMethodName = msg.requester.workflowMethodName,
        workflowMethodId = msg.requester.workflowMethodId!!,
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
