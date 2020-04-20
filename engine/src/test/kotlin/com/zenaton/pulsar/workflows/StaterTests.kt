package com.zenaton.pulsar.workflows

import com.zenaton.engine.workflows.WorkflowState
import com.zenaton.pulsar.workflows.serializers.StateSerDeInterface
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
        // given
        val key = Arb.string(1).toString()
        every { context.getState(key) } returns null
        val dispatcher = Stater(context)
        // when
        val state = dispatcher.getState(key)
        // then
        state shouldBe null
    }

    "Stater.getState state should return deserialized state" {
        // mocking
        val context = mockk<Context>()
        val serDe = mockk<StateSerDeInterface>()
        val mockByteBuffer = mockk<ByteBuffer>()
        val mockWorkflowState = mockk<WorkflowState>()
        // given
        val key = Arb.string(1).toString()
        every { context.getState(key) } returns mockByteBuffer
        every { serDe.deserialize(mockByteBuffer) } returns mockWorkflowState
        val stater = Stater(context)
        stater.serDe = serDe
        // when
        val state = stater.getState(key)
        // then
        state shouldBe mockWorkflowState
    }
})
