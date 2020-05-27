package com.zenaton.taskmanager.pulsar.state

import com.zenaton.commons.utils.TestFactory
import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskmanager.data.TaskId
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
        val taskId = TestFactory.get(TaskId::class)
        // mocking
        val context = mockk<Context>()
        every { context.getState(any()) } returns null
        // given
        val stateStorage = PulsarTaskEngineStateStorage(context)
        // when
        val state = stateStorage.getState(taskId)
        // then
        state shouldBe null
    }

    "PulsarFunctionStateStorageTests.getState state should return deserialize state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(TaskState::class)
        every { context.getState(stateIn.taskId.id) } returns AvroSerDe.serialize(TaskAvroConverter.toAvro(stateIn))
        // given
        val stateStorage = PulsarTaskEngineStateStorage(context)
        // when
        val stateOut = stateStorage.getState(stateIn.taskId)
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
        stateStorage.updateState(stateIn.taskId, stateIn, null)
        // then
        verify(exactly = 1) { context.putState(stateIn.taskId.id, AvroSerDe.serialize(TaskAvroConverter.toAvro(stateIn))) }
        confirmVerified(context)
    }

    "PulsarFunctionStateStorageTests.updateState should record serialized state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(TaskState::class)
        val stateOut = TestFactory.get(TaskState::class)
        every { context.putState(any(), any()) } returns Unit
        // given
        val stateStorage = PulsarTaskEngineStateStorage(context)
        // when
        stateStorage.updateState(stateIn.taskId, stateOut, stateIn)
        // then
        verify(exactly = 1) { context.putState(stateIn.taskId.id, AvroSerDe.serialize(TaskAvroConverter.toAvro(stateOut))) }
        confirmVerified(context)
    }

    "PulsarFunctionStateStorageTests.deleteState should delete state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(TaskState::class)
        every { context.deleteState(any()) } returns Unit
        // given
        val stageStorage = PulsarTaskEngineStateStorage(context)
        // when
        stageStorage.deleteState(stateIn.taskId)
        // then
        verify(exactly = 1) { context.deleteState(stateIn.taskId.id) }
        confirmVerified(context)
    }
})
