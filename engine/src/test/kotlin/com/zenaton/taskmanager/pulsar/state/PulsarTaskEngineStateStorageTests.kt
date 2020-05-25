package com.zenaton.taskmanager.pulsar.state

import com.zenaton.commons.utils.TestFactory
import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskmanager.data.TaskState
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.pulsar.functions.api.Context

class PulsarTaskEngineStateStorageTests : StringSpec({
    "PulsarFunctionStateStorageTests.getState with no state should return null" {
        // mocking
        val context = mockk<Context>()
        every { context.getState(any()) } returns null
        // given
        val stateStorage = PulsarTaskEngineStateStorage(context)
        // when
        val state = stateStorage.getState("key")
        // then
        state shouldBe null
    }

    "PulsarFunctionStateStorageTests.getState state should return deserialize state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(TaskState::class)
        every { context.getState(any()) } returns AvroSerDe.serialize(TaskAvroConverter.toAvro(stateIn))
        // given
        val stateStorage = PulsarTaskEngineStateStorage(context)
        // when
        val stateOut = stateStorage.getState("key")
        // then
        stateOut shouldBe stateIn
    }

    "PulsarFunctionStateStorageTests.createState should record serialized state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(TaskState::class)
        every { context.putState(any(), any()) } returns Unit
        // given
        val stateStorage = PulsarTaskEngineStateStorage(context)
        // when
        val key = TestFactory.get(String::class)
        stateStorage.createState(key, stateIn)
        // then
        verify(exactly = 1) { context.putState(key, AvroSerDe.serialize(TaskAvroConverter.toAvro(stateIn))) }
        confirmVerified(context)
    }

    "PulsarFunctionStateStorageTests.updateState should record serialized state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(TaskState::class)
        every { context.putState(any(), any()) } returns Unit
        // given
        val stateStorage = PulsarTaskEngineStateStorage(context)
        // when
        val key = TestFactory.get(String::class)
        stateStorage.updateState(key, stateIn)
        // then
        verify(exactly = 1) { context.putState(key, AvroSerDe.serialize(TaskAvroConverter.toAvro(stateIn))) }
        confirmVerified(context)
    }

    "PulsarFunctionStateStorageTests.deleteState should delete state" {
        // mocking
        val context = mockk<Context>()
        every { context.deleteState(any()) } returns Unit
        // given
        val stageStorage = PulsarTaskEngineStateStorage(context)
        // when
        val key = TestFactory.get(String::class)
        stageStorage.deleteState(key)
        // then
        verify(exactly = 1) { context.deleteState(key) }
        confirmVerified(context)
    }
})
