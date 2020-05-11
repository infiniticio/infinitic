package com.zenaton.pulsar.utils

import com.zenaton.engine.data.interfaces.StateInterface
import com.zenaton.engine.topics.workflows.state.WorkflowState
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.pulsar.functions.api.Context

class StaterTests : StringSpec({
    "Stater.getState with no state should return null" {
        // mocking
        val context = mockk<Context>()
        // given
        val key = Arb.string(1).toString()
        every { context.getState(key) } returns null
        val dispatcher = Stater<WorkflowState>(context)
        // when
        val state = dispatcher.getState(key)
        // then
        state shouldBe null
    }

    "Stater.getState state should return deserialize state" {
        // mocking
//        val context = mockk<Context>()
//        val serializeState = ByteBuffer.wrap(Arb.string(1).toString().toByteArray())
//        every { context.getState(any()) } returns serializeState
//        val mockState = mockk<StateInterface>()
//        val serDe = mockk<AvroSerDe>()
//        every { serDe.deserialize<StateInterface>(any()) } returns mockState
//        // given
//        val stater = Stater<StateInterface>(context)
//        stater.serDe = serDe
//        // when
//        val key = Arb.string(1).toString()
//        val state = stater.getState(key)
//        // then
//        verify(exactly = 1) { context.getState(key) }
//        verify(exactly = 1) { serDe.deserialize<StateInterface>(serializeState) }
//        confirmVerified(context)
//        confirmVerified(serDe)
//        state shouldBe mockState
    }

    "Stater.createState should record serialized state" {
        // mocking
//        val context = mockk<Context>()
//        every { context.putState(any(), any()) } returns Unit
//        val mockState = mockk<StateInterface>()
//        val serDe = mockk<AvroSerDe>()
//        val serializeState = ByteBuffer.wrap(Arb.string(1).toString().toByteArray())
//        every { serDe.serialize(mockState) } returns serializeState
//        // given
//        val stater = Stater<StateInterface>(context)
//        stater.serDe = serDe
//        // when
//        val key = Arb.string(1).toString()
//        stater.createState(key, mockState)
//        // then
//        verify(exactly = 1) { context.putState(key, serializeState) }
//        confirmVerified(context)
    }

    "Stater.updateState should record serialized state" {
        // mocking
//        val context = mockk<Context>()
//        every { context.putState(any(), any()) } returns Unit
//        val mockState = mockk<StateInterface>()
//        val serDe = mockk<AvroSerDe>()
//        val serializeState = ByteBuffer.wrap(Arb.string(1).toString().toByteArray())
//        every { serDe.serialize(mockState) } returns serializeState
//        // given
//        val stater = Stater<StateInterface>(context)
//        stater.serDe = serDe
//        // when
//        val key = Arb.string(1).toString()
//        stater.updateState(key, mockState)
//        // then
//        verify(exactly = 1) { context.putState(key, serializeState) }
//        confirmVerified(context)
    }

    "f Stater.deleteState should delete state" {
        // mocking
        val context = mockk<Context>()
        every { context.deleteState(any()) } returns Unit
        // given
        val stater = Stater<StateInterface>(context)
        // when
        val key = Arb.string(1).toString()
        stater.deleteState(key)
        // then
        verify(exactly = 1) { context.deleteState(key) }
        confirmVerified(context)
    }
})
