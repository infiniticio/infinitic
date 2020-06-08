package com.zenaton.jobManager.pulsar.functions

import com.zenaton.jobManager.functions.MonitoringGlobalFunction
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.pulsar.storage.PulsarAvroStorage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.apache.pulsar.functions.api.Context

class MonitoringGlobalFunctionTests : ShouldSpec({
    context("MonitoringGlobalPulsarFunction.process") {

        should("throw an exception if called with a null context") {
            val function = MonitoringGlobalPulsarFunction()

            shouldThrow<NullPointerException> {
                function.process(mockk<AvroForMonitoringGlobalMessage>(), null)
            }
        }

        should("call metrics with correct parameters") {
            // mocking
            val context = mockk<Context>()
            every { context.logger } returns mockk<org.slf4j.Logger>()
            val monitoringFunction = spyk<MonitoringGlobalFunction>()
            every { monitoringFunction.handle(any()) } just Runs
            val avroMsg = mockk<AvroForMonitoringGlobalMessage>()
            // given
            val fct = MonitoringGlobalPulsarFunction()
            fct.monitoring = monitoringFunction
            // when
            fct.process(avroMsg, context)
            // then
            monitoringFunction.logger shouldBe context.logger
            (monitoringFunction.avroStorage as PulsarAvroStorage).context shouldBe context
            verify(exactly = 1) { monitoringFunction.handle(avroMsg) }
        }
    }
})
