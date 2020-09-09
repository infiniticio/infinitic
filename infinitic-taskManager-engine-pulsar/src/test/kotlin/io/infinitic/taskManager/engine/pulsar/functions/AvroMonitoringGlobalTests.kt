package io.infinitic.taskManager.engine.pulsar.functions

import io.infinitic.taskManager.engine.avroClasses.AvroMonitoringGlobal
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import io.infinitic.taskManager.storage.pulsar.PulsarStorage
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

class AvroMonitoringGlobalTests : ShouldSpec({
    context("MonitoringGlobalPulsarFunction.process") {

        should("throw an exception if called with a null context") {
            val function = MonitoringGlobalPulsarFunction()

            shouldThrow<NullPointerException> {
                function.process(mockk<AvroEnvelopeForMonitoringGlobal>(), null)
            }
        }

        should("call metrics with correct parameters") {
            // mocking
            val context = mockk<Context>()
            every { context.logger } returns mockk<org.slf4j.Logger>()
            val monitoringFunction = spyk<AvroMonitoringGlobal>()
            every { monitoringFunction.handle(any()) } just Runs
            val avroMsg = mockk<AvroEnvelopeForMonitoringGlobal>()
            // given
            val fct = MonitoringGlobalPulsarFunction()
            fct.monitoring = monitoringFunction
            // when
            fct.process(avroMsg, context)
            // then
            monitoringFunction.logger shouldBe context.logger
            (monitoringFunction.avroStorage as PulsarStorage).context shouldBe context
            verify(exactly = 1) { monitoringFunction.handle(avroMsg) }
        }
    }
})
