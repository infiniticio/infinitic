package com.zenaton.pulsar.workflows

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.apache.pulsar.functions.api.Context

class DispatcherTests : StringSpec({
    "getState with no state should return null" {
        // mocking
        val context = mockk<Context>()
        val slotKey = slot<String>()
        every { context.getState(capture(slotKey)) } returns null
        // given
        val dispatcher = Dispatcher(context)
        // when
        val state = dispatcher.getState("randomKey")
        // then
        val key = slotKey.captured
        verify(exactly = 1) { context.getState(key) }
        confirmVerified(context)
        key shouldBe "randomKey"
        state shouldBe null
    }
})
