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
package io.infinitic.workflows.engine

import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.topics.ClientTopic
import io.infinitic.common.topics.DelayedServiceExecutorTopic
import io.infinitic.common.topics.ServiceExecutorTopic
import io.infinitic.common.topics.ServiceTagTopic
import io.infinitic.common.topics.WorkflowEngineTopic
import io.infinitic.common.topics.WorkflowEventsTopic
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.events.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.kotest.core.spec.style.StringSpec
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

class WorkflowEngineTests : StringSpec(
    {
      // slots
      val stateSlot = slot<WorkflowState>()
      val workflowIdSlot = slot<WorkflowId>()
      val clientSlot = slot<ClientMessage>()
      val eventSlot = slot<WorkflowEventMessage>()
      val taskTagSlots = CopyOnWriteArrayList<ServiceTagMessage>()
      val taskExecutorSlot = slot<ServiceExecutorMessage>()
      val workflowTagSlots = CopyOnWriteArrayList<WorkflowTagMessage>()
      val workflowEngineSlot = slot<WorkflowEngineMessage>()
      val afterSlot = slot<MillisDuration>()

      // mocks
      fun completed() = CompletableFuture.completedFuture(Unit)
      val storage = mockk<WorkflowStateStorage>()
      val producerAsync = mockk<InfiniticProducerAsync> {
        every { producerName } returns "testName"
        every { capture(clientSlot).sendToAsync(ClientTopic) } returns completed()
        every { capture(taskTagSlots).sendToAsync(ServiceTagTopic) } returns completed()
        every { capture(eventSlot).sendToAsync(WorkflowEventsTopic) } returns completed()

        every { capture(taskExecutorSlot).sendToAsync(ServiceExecutorTopic) } returns completed()
        every {
          capture(taskExecutorSlot).sendToAsync(DelayedServiceExecutorTopic, capture(afterSlot))
        } returns completed()
        every { capture(workflowEngineSlot).sendToAsync(WorkflowEngineTopic) } returns completed()
      }

      // ensure slots are emptied between each test
      beforeTest {
        clearMocks(storage)
        coEvery { storage.putState(capture(workflowIdSlot), capture(stateSlot)) } just Runs

        clientSlot.clear()
        taskTagSlots.clear()
        taskExecutorSlot.clear()
        workflowTagSlots.clear()
        workflowEngineSlot.clear()
        afterSlot.clear()
      }

      "Dispatch Workflow" {
//        val msg = TestFactory.random(DispatchNewWorkflow::class, mapOf("parentWorkflowId" to null))
//
//        coEvery { storage.getState(msg.workflowId) } returns null
//
//        coroutineScope { engine.handle(msg) }
//
//        val workflowTask = (taskExecutorSlot.captured as ExecuteTask)
//        workflowTask.workflowVersion shouldBe null
//        stateSlot.captured.workflowVersion shouldBe null
//
//        val returnValue = WorkflowTaskReturnValue(
//            newCommands = listOf(),
//            newStep = null,
//            properties = mapOf(),
//            methodReturnValue = null,
//            workflowVersion = WorkflowVersion(42),
//        )
//
//        val taskCompleted = TaskCompleted(
//            TaskReturnValue(
//                workflowTask.taskId,
//                workflowTask.serviceName,
//                workflowTask.taskMeta,
//                ReturnValue.from(returnValue),
//            ),
//            stateSlot.captured.workflowName,
//            stateSlot.captured.workflowId,
//            stateSlot.captured.runningWorkflowMethodId!!,
//            EmitterName("worker"),
//        )
//
//        coroutineScope { engine.handle(taskCompleted) }
        // todo
      }
    },
)
