package io.infinitic.workflowManager.pulsar.storage

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.workflowManager.pulsar.utils.TestFactory
import io.infinitic.workflowManager.states.AvroWorkfloState
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.pulsar.functions.api.Context

class PulsarAvroStorageTests : ShouldSpec({
    context("PulsarAvroStorage.getWorkflowEngineState") {
        should("return null when state does not exist") {
            val workflowId = TestFactory.random(String::class)
            // mocking
            val context = mockk<Context>()
            every { context.getState(any()) } returns null
            // given
            val stateStorage = PulsarAvroStorage(context)
            // when
            val state = stateStorage.getWorkflowState(workflowId)
            // then
            verify(exactly = 1) { context.getState("engine.state.$workflowId") }
            confirmVerified(context)
            state shouldBe null
        }

        should("return state when state exists") {
            // mocking
            val context = mockk<Context>()
            val stateIn = TestFactory.random(AvroWorkfloState::class)
            every { context.getState(any()) } returns AvroSerDe.serialize(stateIn)
            // given
            val stateStorage = PulsarAvroStorage(context)
            // when
            val stateOut = stateStorage.getWorkflowState(stateIn.workflowId)
            // then
            verify(exactly = 1) { context.getState("engine.state.${stateIn.workflowId}") }
            confirmVerified(context)
            stateOut shouldBe stateIn
        }
    }

    context("PulsarAvroStorage.updateWorkflowEngineState") {
        should("record state") {
            // mocking
            val context = mockk<Context>()
            val stateIn = TestFactory.random(AvroWorkfloState::class)
            every { context.putState(any(), any()) } returns Unit
            // given
            val stateStorage = PulsarAvroStorage(context)
            // when
            stateStorage.updateWorkflowState(stateIn.workflowId, stateIn, null)
            // then
            verify(exactly = 1) {
                context.putState(
                    "engine.state.${stateIn.workflowId}",
                    AvroSerDe.serialize(stateIn)
                )
            }
            confirmVerified(context)
        }
    }

    context("PulsarAvroStorage.deleteWorkflowEngineState") {
        should("delete state") {
            // mocking
            val context = mockk<Context>()
            val stateIn = TestFactory.random(AvroWorkfloState::class)
            every { context.deleteState(any()) } returns Unit
            // given
            val stageStorage = PulsarAvroStorage(context)
            // when
            stageStorage.deleteWorkflowState(stateIn.workflowId)
            // then
            verify(exactly = 1) { context.deleteState("engine.state.${stateIn.workflowId}") }
            confirmVerified(context)
        }
    }
})
