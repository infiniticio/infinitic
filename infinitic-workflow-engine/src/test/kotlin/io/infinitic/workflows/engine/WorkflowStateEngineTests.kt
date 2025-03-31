/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of the rights granted to you
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

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.methods.MethodArgs
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.requester.ClientRequester
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.kotest.core.spec.style.AnnotationSpec.BeforeEach
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

class WorkflowStateEngineTests : StringSpec(
    {
      // Mock dependencies
      val storage = mockk<WorkflowStateStorage>()
      val producer = mockk<InfiniticProducer>(relaxed = true)
      val engine = WorkflowStateEngine(storage, producer)

      // Test slots to capture values
      val stateSlot = slot<WorkflowState>()
      val versionSlot = slot<Long>()
      
      beforeEach {
        clearMocks(storage)
      }

      "process should handle DispatchWorkflow message for new workflow" {
        // Given
        val workflowId = WorkflowId()
        val message = getDispatchWorkflow(workflowId)

        // Mock storage behavior
        coEvery {
          storage.getStateAndVersion(workflowId)
        } returns Pair(null, 0L)

        coEvery {
          storage.putStateWithVersion(workflowId, capture(stateSlot), capture(versionSlot))
        } returns true

        // When
        engine.process(message, message.emittedAt!!)

        // Then
        coVerify { storage.getStateAndVersion(workflowId) }
        coVerify { storage.putStateWithVersion(workflowId, any(), 0L) }

        // Verify state properties
        val capturedState = stateSlot.captured
        capturedState.shouldBeInstanceOf<WorkflowState>()
        capturedState.workflowId shouldBe workflowId
        capturedState.workflowName shouldBe message.workflowName
      }

      "process should retry on version mismatch" {
        // Given
        val workflowId = WorkflowId()
        val message = getDispatchWorkflow(workflowId)

        // Mock storage behavior to fail once then succeed
        coEvery {
          storage.getStateAndVersion(workflowId)
        } returns Pair(null, 0L)

        coEvery {
          storage.putStateWithVersion(workflowId, any(), 0L)
        } returnsMany listOf(false, true)

        // When
        engine.process(message, message.emittedAt!!)

        // Then
        coVerify(exactly = 2) { storage.putStateWithVersion(workflowId, any(), 0L) }
      }

      "process should handle race condition with null state" {
        // Given
        val workflowId = WorkflowId()
        val workflowName = WorkflowName("TestWorkflow")
        val message = mockk<WorkflowStateEngineMessage>(relaxed = true) {
          every { this@mockk.workflowId } returns workflowId
          every { this@mockk.workflowName } returns workflowName
          every { this@mockk.isWorkflowTaskEvent() } returns false
        }
        val publishTime = MillisInstant.now()

        // Mock storage behavior
        coEvery {
          storage.getStateAndVersion(workflowId)
        } returns Pair(null, 0L)

        coEvery {
          storage.putStateWithVersion(workflowId, capture(stateSlot), capture(versionSlot))
        } returns true

        // When
        engine.process(message, publishTime)

        // Then
        coVerify { storage.getStateAndVersion(workflowId) }

        // Verify state properties for race condition
        val capturedState = stateSlot.captured
        capturedState.shouldBeInstanceOf<WorkflowState>()
        capturedState.workflowId shouldBe workflowId
        capturedState.workflowName shouldBe workflowName
        capturedState.runningWorkflowTaskId?.toString() shouldBe WorkflowStateEngine.UNDEFINED_DUE_TO_RACE_CONDITION
      }

      "batchProcess should handle multiple messages for different workflows" {
        // Given
        val workflowId1 = WorkflowId()
        val workflowId2 = WorkflowId()
        val message1 = getDispatchWorkflow(workflowId1)
        val message2 = getDispatchWorkflow(workflowId2)
        val messages = listOf(
            Pair(message1, message1.emittedAt!!),
            Pair(message2, message2.emittedAt!!),
        )

        // Mock storage behavior
        coEvery {
          storage.getStatesAndVersions(any())
        } returns mapOf(
            workflowId1 to Pair(null, 0L),
            workflowId2 to Pair(null, 0L),
        )

        coEvery {
          storage.putStatesWithVersions(any())
        } returns mapOf(
            workflowId1 to true,
            workflowId2 to true,
        )

        // When
        engine.batchProcess(messages)

        // Then
        coVerify { storage.getStatesAndVersions(listOf(workflowId1, workflowId2)) }
        coVerify { storage.putStatesWithVersions(any()) }
      }

      "batchProcess should retry failed updates" {
        // Given
        val workflowId1 = WorkflowId()
        val workflowId2 = WorkflowId()
        val message1 = getDispatchWorkflow(workflowId1)
        val message2 = getDispatchWorkflow(workflowId2)
        val messages = listOf(
            Pair(message1, message1.emittedAt!!),
            Pair(message2, message2.emittedAt!!),
        )

        // Mock storage behavior - first attempt fails for workflowId1
        coEvery {
          storage.getStatesAndVersions(any())
        } returnsMany listOf(
            mapOf(
                workflowId1 to Pair(null, 0L),
                workflowId2 to Pair(null, 0L),
            ),
            mapOf(
                workflowId1 to Pair(null, 0L),
            ),
        )

        coEvery {
          storage.putStatesWithVersions(any())
        } returnsMany listOf(
            mapOf(
                workflowId1 to false,
                workflowId2 to true,
            ),
            mapOf(
                workflowId1 to true,
            ),
        )

        // When
        engine.batchProcess(messages)

        // Then
        coVerify(exactly = 2) { storage.getStatesAndVersions(any()) }
        coVerify(exactly = 2) { storage.putStatesWithVersions(any()) }
      }

      "batchProcess should handle empty message list" {
        // Given
        val messages = emptyList<Pair<WorkflowStateEngineMessage, MillisInstant>>()

        // When
        engine.batchProcess(messages)

        // Then
        coVerify(exactly = 0) { storage.getStatesAndVersions(any()) }
        coVerify(exactly = 0) { storage.putStatesWithVersions(any()) }
      }
    },
)

private fun getDispatchWorkflow(workflowId: WorkflowId): DispatchWorkflow {
  val workflowName = WorkflowName("TestWorkflow")
  val emitterName = EmitterName("testEmitter")
  val clientName = ClientName("testClient")
  val publishTime = MillisInstant.now()

  return DispatchWorkflow(
      workflowId = workflowId,
      workflowName = workflowName,
      methodName = MethodName("testMethod"),
      methodParameters = MethodArgs(),
      methodParameterTypes = MethodParameterTypes(listOf()),
      workflowTaskId = TaskId("current"),
      workflowTags = setOf(WorkflowTag("testTag")),
      workflowMeta = WorkflowMeta(),
      clientWaiting = true,
      emitterName = emitterName,
      requester = ClientRequester(clientName = clientName),
      emittedAt = publishTime,
  )
}
