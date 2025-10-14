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
package io.infinitic.clients

import io.infinitic.clients.config.InfiniticClientConfig
import io.infinitic.common.data.MessageId
import io.infinitic.common.utils.IdGenerator
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.storage.config.InMemoryStorageConfig
import io.infinitic.transport.config.InMemoryTransportConfig
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking

class InfiniticClientGetWorkflowStateTest :
  StringSpec(
      {
        val transport = InMemoryTransportConfig()
        val storage = InMemoryStorageConfig.builder().build()

        val configWithStorage =
            InfiniticClientConfig(name = "test-client", transport = transport, storage = storage)

        val configWithoutStorage =
            InfiniticClientConfig(name = "test-client-no-storage", transport = transport)

        val clientWithStorage = InfiniticClient(configWithStorage)
        val clientWithoutStorage = InfiniticClient(configWithoutStorage)

        afterSpec {
          clientWithStorage.close()
          clientWithoutStorage.close()
        }

        "getWorkflowState should throw when storage is not configured" {
          val exception = shouldThrow<java.util.concurrent.CompletionException> {
            clientWithoutStorage.getWorkflowStateById(IdGenerator.next())
          }
          exception.cause.shouldBeInstanceOf<IllegalStateException>()
        }

        "getWorkflowStateAsync should throw when storage is not configured" {
          shouldThrow<IllegalStateException> {
            clientWithoutStorage.getWorkflowStateByIdAsync(IdGenerator.next()).await()
          }
        }

        "getWorkflowState should return null for non-existent workflow" {
          val workflowId = IdGenerator.next()
          val state = clientWithStorage.getWorkflowStateById(workflowId)

          state.shouldBeNull()
        }

        "getWorkflowStateAsync should return null for non-existent workflow" {
          val workflowId = IdGenerator.next()
          val state = clientWithStorage.getWorkflowStateByIdAsync(workflowId).await()

          state.shouldBeNull()
        }

        "getWorkflowState should return state for existing workflow" {
          val workflowId = WorkflowId(IdGenerator.next())
          val workflowName = WorkflowName("TestWorkflow")

          // Create a workflow state
          val expectedState =
              WorkflowState(
                  lastMessageId = MessageId(IdGenerator.next()),
                  workflowId = workflowId,
                  workflowName = workflowName,
                  workflowVersion = WorkflowVersion(1),
                  workflowTags = setOf(),
                  workflowMeta = WorkflowMeta(),
                  runningWorkflowTaskId = null,
                  runningWorkflowMethodId = null,
                  positionInRunningWorkflowMethod = null,
                  workflowMethods = mutableListOf(),
              )

          // Store the state directly using the internal storage
          val workflowStorage = BinaryWorkflowStateStorage(storage.keyValue)
          runBlocking { workflowStorage.putStateWithVersion(workflowId, expectedState, 0) }

          // Retrieve the state using the client method
          val retrievedState = clientWithStorage.getWorkflowStateById(workflowId.toString())

          // Verify the retrieved state matches
          retrievedState shouldBe expectedState
        }

        "getWorkflowStateAsync should return state for existing workflow" {
          val workflowId = WorkflowId(IdGenerator.next())
          val workflowName = WorkflowName("TestWorkflow")

          // Create a workflow state
          val expectedState =
              WorkflowState(
                  lastMessageId = MessageId(IdGenerator.next()),
                  workflowId = workflowId,
                  workflowName = workflowName,
                  workflowVersion = WorkflowVersion(1),
                  workflowTags = setOf(),
                  workflowMeta = WorkflowMeta(),
                  runningWorkflowTaskId = null,
                  runningWorkflowMethodId = null,
                  positionInRunningWorkflowMethod = null,
                  workflowMethods = mutableListOf(),
              )

          // Store the state directly using the internal storage
          val workflowStorage = BinaryWorkflowStateStorage(storage.keyValue)
          runBlocking { workflowStorage.putStateWithVersion(workflowId, expectedState, 0) }

          // Retrieve the state using the async client method
          val retrievedState =
              clientWithStorage.getWorkflowStateByIdAsync(workflowId.toString()).await()

          // Verify the retrieved state matches
          retrievedState shouldBe expectedState
        }

        "getWorkflowStateSuspend should throw when storage is not configured" {
          shouldThrow<IllegalStateException> {
            runBlocking { clientWithoutStorage.getWorkflowStateByIdSuspend(IdGenerator.next()) }
          }
        }

        "getWorkflowStateSuspend should return null for non-existent workflow" {
          val workflowId = IdGenerator.next()
          val state = runBlocking { clientWithStorage.getWorkflowStateByIdSuspend(workflowId) }

          state.shouldBeNull()
        }

        "getWorkflowStateSuspend should return state for existing workflow" {
          val workflowId = WorkflowId(IdGenerator.next())
          val workflowName = WorkflowName("TestWorkflow")

          // Create a workflow state
          val expectedState =
              WorkflowState(
                  lastMessageId = MessageId(IdGenerator.next()),
                  workflowId = workflowId,
                  workflowName = workflowName,
                  workflowVersion = WorkflowVersion(1),
                  workflowTags = setOf(),
                  workflowMeta = WorkflowMeta(),
                  runningWorkflowTaskId = null,
                  runningWorkflowMethodId = null,
                  positionInRunningWorkflowMethod = null,
                  workflowMethods = mutableListOf(),
              )

          // Store the state directly using the internal storage
          val workflowStorage = BinaryWorkflowStateStorage(storage.keyValue)
          runBlocking { workflowStorage.putStateWithVersion(workflowId, expectedState, 0) }

          // Retrieve the state using the suspend client method
          val retrievedState =
              runBlocking { clientWithStorage.getWorkflowStateByIdSuspend(workflowId.toString()) }

          // Verify the retrieved state matches
          retrievedState shouldBe expectedState
        }
      },
  )
