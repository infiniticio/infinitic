package com.zenaton.pulsar.workflows

import com.zenaton.engine.workflows.WorkflowState
import com.zenaton.pulsar.serializer.MessageSerDeInterface
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.mockk.every
import io.mockk.mockk
import java.nio.ByteBuffer
import org.apache.pulsar.functions.api.Context

class StaterTests : StringSpec({
    "Stater.getState with no state should return null" {
        // mocking
        val context = mockk<Context>()
        val serde = mockk<MessageSerDeInterface>()
        // given
        val key = Arb.string(1).toString()
        every { context.getState(key) } returns null
        val dispatcher = Stater(context, serde)
        // when
        val state = dispatcher.getState(key)
        // then
        state shouldBe null
    }

    "Stater.getState state should return deserialized state" {
        // mocking
        val context = mockk<Context>()
        val serde = mockk<MessageSerDeInterface>()
        val mockByteBuffer = mockk<ByteBuffer>()
        val mockWorkflowState = mockk<WorkflowState>()
        // given
        val key = Arb.string(1).toString()
        every { context.getState(key) } returns mockByteBuffer
        every { serde.deSerializeState(mockByteBuffer) } returns mockWorkflowState
        val dispatcher = Stater(context, serde)
        // when
        val state = dispatcher.getState(key)
        // then
        state shouldBe mockWorkflowState
    }
})
