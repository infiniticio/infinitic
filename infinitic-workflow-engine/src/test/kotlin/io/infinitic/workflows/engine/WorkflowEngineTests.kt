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
import io.infinitic.common.fixtures.mockSendToClient
import io.infinitic.common.fixtures.mockSendToTaskExecutor
import io.infinitic.common.fixtures.mockSendToTaskTag
import io.infinitic.common.fixtures.mockSendToWorkflowEngine
import io.infinitic.common.fixtures.mockSendToWorkflowEngineAfter
import io.infinitic.common.fixtures.mockSendToWorkflowTag
import io.infinitic.common.fixtures.mockSendToWorkflowTaskExecutor
import io.infinitic.common.tasks.data.TaskReturnValue
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskReturnValue
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
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
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.coroutineScope

class WorkflowEngineTests :
    StringSpec({
      val storage = mockk<WorkflowStateStorage>()
      val state = slot<WorkflowState>()
      val workflowId = slot<WorkflowId>()

      val clientMessage = slot<ClientMessage>()
      val taskTagMessages = CopyOnWriteArrayList<TaskTagMessage>()
      val taskExecutorMessage = slot<TaskExecutorMessage>()
      val workflowTaskExecutorMessage = slot<TaskExecutorMessage>()
      val workflowTagMessages = CopyOnWriteArrayList<WorkflowTagMessage>()
      val workflowEngineMessage = slot<WorkflowEngineMessage>()
      val after = slot<MillisDuration>()

      val engine =
          WorkflowEngine(
              ClientName("test"),
              storage,
              mockSendToClient(clientMessage),
              mockSendToTaskTag(taskTagMessages),
              mockSendToTaskExecutor(taskExecutorMessage),
              mockSendToWorkflowTaskExecutor(workflowTaskExecutorMessage),
              mockSendToWorkflowTag(workflowTagMessages),
              mockSendToWorkflowEngine(workflowEngineMessage),
              mockSendToWorkflowEngineAfter(workflowEngineMessage, after))

      // ensure slots are emptied between each test
      beforeTest {
        clearMocks(storage)
        coEvery { storage.putState(capture(workflowId), capture(state)) } just Runs

        clientMessage.clear()
        taskTagMessages.clear()
        taskExecutorMessage.clear()
        workflowTaskExecutorMessage.clear()
        workflowTagMessages.clear()
        workflowEngineMessage.clear()
        after.clear()
      }

      "Dispatch Workflow" {
        val msg = TestFactory.random(DispatchWorkflow::class)
        coEvery { storage.getState(msg.workflowId) } returns null

        coroutineScope { engine.handle(msg) }

        val workflowTask = (workflowTaskExecutorMessage.captured as ExecuteTask)
        workflowTask.workflowVersion shouldBe null
        state.captured.workflowVersion shouldBe null

        val returnValue =
            WorkflowTaskReturnValue(
                newCommands = listOf(),
                newStep = null,
                properties = mapOf(),
                methodReturnValue = null,
                workflowVersion = WorkflowVersion(42))
        val taskCompleted =
            TaskCompleted(
                state.captured.workflowName,
                state.captured.workflowId,
                state.captured.runningMethodRunId!!,
                TaskReturnValue(
                    workflowTask.taskId,
                    workflowTask.serviceName,
                    workflowTask.taskMeta,
                    ReturnValue.from(returnValue)),
                ClientName("worker"))

        coroutineScope { engine.handle(taskCompleted) }
        // todo
      }
    })
