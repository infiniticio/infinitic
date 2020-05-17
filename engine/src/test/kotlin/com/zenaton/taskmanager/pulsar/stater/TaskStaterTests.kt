package com.zenaton.taskmanager.pulsar.stater

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.commons.pulsar.utils.Stater
import com.zenaton.commons.utils.TestFactory
import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.taskmanager.state.TaskState
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.pulsar.functions.api.Context

class TaskStaterTests : StringSpec({
    "TaskStater.getState with no state should return null" {
        // mocking
        val context = mockk<Context>()
        val key = TestFactory.get(String::class)
        every { context.getState(key) } returns null
        // given
        val stater = TaskStater(context)
        // when
        val state = stater.getState(key)
        // then
        state shouldBe null
    }

    "Stater.getState state should return deserialize state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(TaskState::class)
        val byteBuffer = AvroSerDe.serialize(TaskAvroConverter.toAvro(stateIn))
        every { context.getState(any()) } returns byteBuffer
        // given
        val stater = TaskStater(context)
        // when
        val stateOut = stater.getState("key")
        // then
        stateOut shouldBe stateIn
    }

    "Stater.createState should record serialized state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(TaskState::class)
        val byteBuffer = AvroSerDe.serialize(TaskAvroConverter.toAvro(stateIn))
        every { context.putState(any(), any()) } returns Unit
        // given
        val stater = TaskStater(context)
        // when
        val key = TestFactory.get(String::class)
        stater.createState(key, stateIn)
        // then
        verify(exactly = 1) { context.putState(key, byteBuffer) }
        confirmVerified(context)
    }

    "Stater.updateState should record serialized state" {
        // mocking
        val context = mockk<Context>()
        val stateIn = TestFactory.get(TaskState::class)
        val byteBuffer = AvroSerDe.serialize(TaskAvroConverter.toAvro(stateIn))
        every { context.putState(any(), any()) } returns Unit
        // given
        val stater = TaskStater(context)
        // when
        val key = TestFactory.get(String::class)
        stater.updateState(key, stateIn)
        // then
        verify(exactly = 1) { context.putState(key, byteBuffer) }
        confirmVerified(context)
    }

    "Stater.deleteState should delete state" {
        // mocking
        val context = mockk<Context>()
        every { context.deleteState(any()) } returns Unit
        // given
        val stater = Stater<StateInterface>(context)
        // when
        val key = TestFactory.get(String::class)
        stater.deleteState(key)
        // then
        verify(exactly = 1) { context.deleteState(key) }
        confirmVerified(context)
    }
})
