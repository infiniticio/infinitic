package com.zenaton.taskManager.pulsar.engine

import com.zenaton.commons.utils.TestFactory
import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskManager.data.TaskId
import com.zenaton.taskManager.engine.EngineState
import com.zenaton.taskManager.pulsar.avro.AvroConverter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.pulsar.functions.api.Context

class EnginePulsarStorageTests : StringSpec({
    "PulsarTaskEngineStateStorageTests.getState with no state should return null" {
        val taskId = TestFactory.get(TaskId::class)
        // mocking
        val context = mockk<Context>()
        every { context.getState(any()) } returns null
        // given
        val stateStorage =
            EnginePulsarStorage(context)
        // when
        val state = stateStorage.getState(taskId)
        // then
        state shouldBe null
    }

    "PulsarTaskEngineStateStorageTests.getState state should return deserialize state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(EngineState::class)
        every { context.getState(stateIn.taskId.id) } returns AvroSerDe.serialize(AvroConverter.toAvro(stateIn))
        // given
        val stateStorage =
            EnginePulsarStorage(context)
        // when
        val stateOut = stateStorage.getState(stateIn.taskId)
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
        stateStorage.updateState(stateIn.taskId, stateIn, null)
        // then
        verify(exactly = 1) {
            context.putState(
                stateIn.taskId.id,
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
        stateStorage.updateState(stateIn.taskId, stateOut, stateIn)
        // then
        verify(exactly = 1) {
            context.putState(
                stateIn.taskId.id,
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
        stageStorage.deleteState(stateIn.taskId)
        // then
        verify(exactly = 1) { context.deleteState(stateIn.taskId.id) }
        confirmVerified(context)
    }
})
