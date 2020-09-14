package io.infinitic.taskManager.engine.pulsar.functions

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.engine.engines.MonitoringGlobal
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.apache.pulsar.functions.api.Context

class MonitoringGlobalPulsarFunctionTests : ShouldSpec({
    context("MonitoringGlobalPulsarFunction.process") {

        should("throw an exception if called with a null context") {
            val function = MonitoringGlobalPulsarFunction()

            shouldThrow<NullPointerException> {
                function.process(mockk(), null)
            }
        }

        should("call metrics with correct parameters") {
            // mocking
            val context = mockk<Context>()
            every { context.logger } returns mockk()
            val monitoringFunction = spyk<MonitoringGlobal>()
            every { monitoringFunction.handle(any()) } just Runs
            val avroMsg = mockk<AvroEnvelopeForMonitoringGlobal>()
            mockkObject(AvroConverter)
            every { AvroConverter.fromMonitoringGlobal(any()) } returns mockk()
            // given
            val fct = MonitoringGlobalPulsarFunction()
            fct.monitoring = monitoringFunction
            // when
            fct.process(avroMsg, context)
            // then
            monitoringFunction.logger shouldBe context.logger
            verify(exactly = 1) { monitoringFunction.handle(any()) }
            unmockkAll()
        }
    }
})
