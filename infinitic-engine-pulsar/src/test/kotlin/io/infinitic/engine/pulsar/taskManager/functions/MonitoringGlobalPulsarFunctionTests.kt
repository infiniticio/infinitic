package io.infinitic.engine.pulsar.taskManager.functions

import io.infinitic.common.tasks.avro.AvroConverter
import io.infinitic.common.tasks.messages.ForMonitoringGlobalMessage
import io.infinitic.engine.taskManager.engines.MonitoringGlobal
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.Runs
import io.mockk.coEvery
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
            // mocking Context
            val context = mockk<Context>()
            every { context.logger } returns mockk()

            // Mocking avro conversion
            val avroMsg = mockk<AvroEnvelopeForMonitoringGlobal>()
            val msg = mockk<ForMonitoringGlobalMessage>()
            mockkObject(AvroConverter)
            every { AvroConverter.fromMonitoringGlobal(avroMsg) } returns msg

            // Mocking Task Engine
            val monitoringGlobal = mockk<MonitoringGlobal>()
            val monitoringGlobalPulsarFunction = spyk<MonitoringGlobalPulsarFunction>()
            every { monitoringGlobalPulsarFunction.getMonitoringGlobal(context) } returns monitoringGlobal
            coEvery { monitoringGlobal.handle(msg) } just Runs

            // when
            monitoringGlobalPulsarFunction.process(avroMsg, context)
            // then
            verify(exactly = 1) { monitoringGlobal.handle(msg) }
            unmockkAll()
        }
    }
})
