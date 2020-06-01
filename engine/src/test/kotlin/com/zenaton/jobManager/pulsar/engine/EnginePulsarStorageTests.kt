package com.zenaton.jobManager.pulsar.engine

import com.zenaton.commons.utils.TestFactory
import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.engine.EngineState
import com.zenaton.jobManager.pulsar.avro.AvroConverter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.pulsar.functions.api.Context

class EnginePulsarStorageTests : StringSpec({
    "PulsarTaskEngineStateStorageTests.getState with no state should return null" {
        val jobId = TestFactory.get(JobId::class)
        // mocking
        val context = mockk<Context>()
        every { context.getState(any()) } returns null
        // given
        val stateStorage =
            EnginePulsarStorage(context)
        // when
        val state = stateStorage.getState(jobId)
        // then
        state shouldBe null
    }

    "PulsarTaskEngineStateStorageTests.getState state should return deserialize state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(EngineState::class)
        every { context.getState(stateIn.jobId.id) } returns AvroSerDe.serialize(AvroConverter.toAvro(stateIn))
        // given
        val stateStorage =
            EnginePulsarStorage(context)
        // when
        val stateOut = stateStorage.getState(stateIn.jobId)
        // then
        stateOut shouldBe stateIn
    }

    "PulsarTaskEngineStateStorageTests.createState should record serialized state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(EngineState::class)
        every { context.putState(any(), any()) } returns Unit
        // given
        val stateStorage =
            EnginePulsarStorage(context)
        // when
        stateStorage.updateState(stateIn.jobId, stateIn, null)
        // then
        verify(exactly = 1) {
            context.putState(
                stateIn.jobId.id,
                AvroSerDe.serialize(AvroConverter.toAvro(stateIn))
            )
        }
        confirmVerified(context)
    }

    "PulsarTaskEngineStateStorageTests.updateState should record serialized state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(EngineState::class)
        val stateOut = TestFactory.get(EngineState::class)
        every { context.putState(any(), any()) } returns Unit
        // given
        val stateStorage =
            EnginePulsarStorage(context)
        // when
        stateStorage.updateState(stateIn.jobId, stateOut, stateIn)
        // then
        verify(exactly = 1) {
            context.putState(
                stateIn.jobId.id,
                AvroSerDe.serialize(AvroConverter.toAvro(stateOut))
            )
        }
        confirmVerified(context)
    }

    "PulsarTaskEngineStateStorageTests.deleteState should delete state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(EngineState::class)
        every { context.deleteState(any()) } returns Unit
        // given
        val stageStorage =
            EnginePulsarStorage(context)
        // when
        stageStorage.deleteState(stateIn.jobId)
        // then
        verify(exactly = 1) { context.deleteState(stateIn.jobId.id) }
        confirmVerified(context)
    }
})
