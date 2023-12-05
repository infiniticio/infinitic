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
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.data.TaskReturnValue
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskReturnValue
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.messages.DispatchNewWorkflow
import io.infinitic.common.workflows.engine.messages.TaskCompleted
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

class WorkflowEngineTests :
  StringSpec(
      {
        // slots
        val stateSlot = slot<WorkflowState>()
        val workflowIdSlot = slot<WorkflowId>()
        val clientSlot = slot<ClientMessage>()
        val taskTagSlots = CopyOnWriteArrayList<TaskTagMessage>()
        val taskExecutorSlot = slot<TaskExecutorMessage>()
        val workflowTagSlots = CopyOnWriteArrayList<WorkflowTagMessage>()
        val workflowEngineSlot = slot<WorkflowEngineMessage>()
        val afterSlot = slot<MillisDuration>()

        // mocks
        fun completed() = CompletableFuture.completedFuture(Unit)
        val storage = mockk<WorkflowStateStorage>()
        val producer = mockk<InfiniticProducerAsync> {
          every { name } returns "testName"
          every { sendAsync(capture(clientSlot)) } returns completed()
          every { sendAsync(capture(taskTagSlots)) } returns completed()
          every { sendAsync(capture(taskExecutorSlot), capture(afterSlot)) } returns completed()
          every { sendAsync(capture(workflowEngineSlot)) } returns completed()
        }

        val engine = WorkflowEngine(storage, producer)

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
          val msg = TestFactory.random(DispatchNewWorkflow::class)
          coEvery { storage.getState(msg.workflowId) } returns null

          coroutineScope { engine.handle(msg) }

          val workflowTask = (taskExecutorSlot.captured as ExecuteTask)
          workflowTask.workflowVersion shouldBe null
          stateSlot.captured.workflowVersion shouldBe null

          val returnValue =
              WorkflowTaskReturnValue(
                  newCommands = listOf(),
                  newStep = null,
                  properties = mapOf(),
                  methodReturnValue = null,
                  workflowVersion = WorkflowVersion(42),
              )
          val taskCompleted =
              TaskCompleted(
                  stateSlot.captured.workflowName,
                  stateSlot.captured.workflowId,
                  stateSlot.captured.runningMethodRunId!!,
                  TaskReturnValue(
                      workflowTask.taskId,
                      workflowTask.serviceName,
                      workflowTask.taskMeta,
                      ReturnValue.from(returnValue),
                  ),
                  ClientName("worker"),
              )

          coroutineScope { engine.handle(taskCompleted) }
          // todo
        }
      },
  )
