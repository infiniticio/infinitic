package com.zenaton.taskmanager.pulsar.storage

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.commons.pulsar.utils.Stater
import com.zenaton.commons.utils.TestFactory
import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskState
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.taskmanager.pulsar.state.PulsarTaskEngineStateStorage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.pulsar.functions.api.Context

class PulsarTaskEngineStateStorageTests : StringSpec({
    "TaskStater.getState with no state should return null" {
        val taskId = TestFactory.get(TaskId::class)
        // mocking
        val context = mockk<Context>()
        every { context.getState(any()) } returns null
        // given
        val storage = PulsarTaskEngineStateStorage(context)
        // when
        val state = storage.getState(taskId)
        // then
        state shouldBe null
    }

    "TaskStater.getState state should return deserialize state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(TaskState::class)
        every { context.getState(stateIn.taskId.id) } returns AvroSerDe.serialize(TaskAvroConverter.toAvro(stateIn))
        // given
        val storage = PulsarTaskEngineStateStorage(context)
        // when
        val stateOut = storage.getState(stateIn.taskId)
        // then
        stateOut shouldBe stateIn
    }

    "TaskStater.createState should record serialized state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(TaskState::class)
        every { context.putState(any(), any()) } returns Unit
        // given
        val storage = PulsarTaskEngineStateStorage(context)
        // when
        storage.updateState(stateIn.taskId, stateIn, null)
        // then
        verify(exactly = 1) { context.putState(stateIn.taskId.id, AvroSerDe.serialize(TaskAvroConverter.toAvro(stateIn))) }
        confirmVerified(context)
    }

    "TaskStater.updateState should record serialized state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(TaskState::class)
        val stateOut = TestFactory.get(TaskState::class)
        every { context.putState(any(), any()) } returns Unit
        // given
        val storage = PulsarTaskEngineStateStorage(context)
        // when
        storage.updateState(stateIn.taskId, stateOut, stateIn)
        // then
        verify(exactly = 1) { context.putState(stateIn.taskId.id, AvroSerDe.serialize(TaskAvroConverter.toAvro(stateOut))) }
        confirmVerified(context)
    }

    "TaskStater.deleteState should delete state" {
        // mocking
        val context = mockk<Context>()
        every { context.deleteState(any()) } returns Unit
        // given
        val storage = Stater<StateInterface>(context)
        // when
        val key = TestFactory.get(String::class)
        storage.deleteState(key)
        // then
        verify(exactly = 1) { context.deleteState(key) }
        confirmVerified(context)
    }
})
