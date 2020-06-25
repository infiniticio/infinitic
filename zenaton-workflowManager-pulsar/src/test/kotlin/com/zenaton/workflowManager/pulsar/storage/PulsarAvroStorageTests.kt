package com.zenaton.workflowManager.pulsar.storage

import com.zenaton.common.avro.AvroSerDe
import com.zenaton.workflowManager.pulsar.utils.TestFactory
import com.zenaton.workflowManager.states.AvroWorkflowEngineState
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
            val state = stateStorage.getWorkflowEngineState(workflowId)
            // then
            verify(exactly = 1) { context.getState("engine.state.$workflowId") }
            confirmVerified(context)
            state shouldBe null
        }

        should("return state when state exists") {
            // mocking
            val context = mockk<Context>()
            val stateIn = TestFactory.random(AvroWorkflowEngineState::class)
            every { context.getState(any()) } returns AvroSerDe.serialize(stateIn)
            // given
            val stateStorage = PulsarAvroStorage(context)
            // when
            val stateOut = stateStorage.getWorkflowEngineState(stateIn.workflowId)
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
            val stateIn = TestFactory.random(AvroWorkflowEngineState::class)
            every { context.putState(any(), any()) } returns Unit
            // given
            val stateStorage = PulsarAvroStorage(context)
            // when
            stateStorage.updateWorkflowEngineState(stateIn.workflowId, stateIn, null)
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
            val stateIn = TestFactory.random(AvroWorkflowEngineState::class)
            every { context.deleteState(any()) } returns Unit
            // given
            val stageStorage = PulsarAvroStorage(context)
            // when
            stageStorage.deleteWorkflowEngineState(stateIn.workflowId)
            // then
            verify(exactly = 1) { context.deleteState("engine.state.${stateIn.workflowId}") }
            confirmVerified(context)
        }
    }
})
