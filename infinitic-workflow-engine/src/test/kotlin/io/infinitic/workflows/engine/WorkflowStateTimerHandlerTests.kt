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

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.withoutDelay
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.RemoteTimerCompleted
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant

class WorkflowStateTimerHandlerTests : StringSpec(
    {
      // Mock dependencies
      val producer = mockk<InfiniticProducer>(relaxed = true) {
        coEvery { internalSendTo(any<WorkflowStateEngineMessage>(), any(), any()) } returns Unit
      }
      val handler = WorkflowStateTimerHandler(producer)

      "process should forward valid RemoteTimerCompleted message to WorkflowStateEngineTopic" {
        // Given
        val workflowId = WorkflowId()
        val workflowName = WorkflowName("TestWorkflow")
        val workflowMethodId = WorkflowMethodId()
        val timerId = TimerId()
        val emitterName = EmitterName("TestEmitter")
        val emittedAt = MillisInstant.now()

        val message = RemoteTimerCompleted(
            timerId = timerId,
            workflowName = workflowName,
            workflowId = workflowId,
            workflowVersion = null,
            workflowMethodName = MethodName("testMethod"),
            workflowMethodId = workflowMethodId,
            emitterName = emitterName,
            emittedAt = emittedAt,
        )

        // When
        handler.process(message, emittedAt)

        // Then
        coVerify { producer.internalSendTo(message, WorkflowStateEngineTopic, MillisDuration(0)) }
      }

      "process should discard RemoteTimerCompleted message that is too old" {
        // Given
        val workflowId = WorkflowId()
        val workflowName = WorkflowName("TestWorkflow")
        val workflowMethodId = WorkflowMethodId()
        val timerId = TimerId()
        val emitterName = EmitterName("TestEmitter")

        // Create a timestamp that is older than MAX_REMOTE_TIMER_AGE_MS (72 hours)
        val oldTimestamp = Instant.now().toEpochMilli() - (4 * 24 * 60 * 60 * 1000) // 96 hours ago
        val emittedAt = MillisInstant(oldTimestamp)

        val message = RemoteTimerCompleted(
            timerId = timerId,
            workflowName = workflowName,
            workflowId = workflowId,
            workflowVersion = null,
            workflowMethodName = MethodName("testMethod"),
            workflowMethodId = workflowMethodId,
            emitterName = emitterName,
            emittedAt = emittedAt,
        )

        // When
        handler.process(message, emittedAt)

        // Then
        // Verify that the message was not forwarded to WorkflowStateEngineTopic
        coVerify(exactly = 0) { producer.internalSendTo(message, any()) }
      }

      "process should forward non-RemoteTimerCompleted message to WorkflowStateEngineTopic" {
        // Given
        val workflowId = WorkflowId()
        val workflowName = WorkflowName("TestWorkflow")
        val emitterName = EmitterName("TestEmitter")
        val emittedAt = MillisInstant.now() - MillisDuration(10 * 24 * 3600 * 1000) // 10 days ago

        // Create a mock of a different WorkflowStateEngineMessage type
        val message = mockk<WorkflowStateEngineMessage>(relaxed = true) {
          every { this@mockk.workflowId } returns workflowId
          every { this@mockk.workflowName } returns workflowName
          every { this@mockk.emitterName } returns emitterName
          every { this@mockk.emittedAt } returns emittedAt
        }

        // When
        handler.process(message, emittedAt)

        // Then
        coVerify { producer.internalSendTo(message, WorkflowStateEngineTopic.withoutDelay) }
      }
    },
)
