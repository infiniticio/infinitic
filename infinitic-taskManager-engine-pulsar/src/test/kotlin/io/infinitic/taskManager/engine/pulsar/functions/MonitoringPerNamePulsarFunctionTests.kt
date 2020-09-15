package io.infinitic.taskManager.engine.pulsar.functions

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.messages.ForMonitoringPerNameMessage
import io.infinitic.taskManager.engine.engines.MonitoringPerName
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import org.apache.pulsar.functions.api.Context

class MonitoringPerNamePulsarFunctionTests : ShouldSpec({
    context("MonitoringPerNamePulsarFunctionTests.process") {

        should("throw an exception if called with a null context") {
            val function = MonitoringPerNamePulsarFunction()

            shouldThrow<NullPointerException> {
                function.process(mockk(), null)
            }
        }

        should("call metrics with correct parameters") {
            // mocking Context
            val context = mockk<Context>()
            every { context.logger } returns mockk()

            // Mocking avro conversion
            val avroMsg = mockk<AvroEnvelopeForMonitoringPerName>()
            val msg = mockk<ForMonitoringPerNameMessage>()
            mockkObject(AvroConverter)
            every { AvroConverter.fromMonitoringPerName(avroMsg) } returns msg

            // Mocking Monitoring Per Name
            val monitoringPerName = mockk<MonitoringPerName>()
            val monitoringPerNamePulsarFunction = spyk<MonitoringPerNamePulsarFunction>()
            every { monitoringPerNamePulsarFunction.getMonitoringPerName(context) } returns monitoringPerName
            coEvery { monitoringPerName.handle(msg) } just Runs

            // when
            monitoringPerNamePulsarFunction.process(avroMsg, context)
            // then
            coVerify(exactly = 1) { monitoringPerName.handle(msg) }
            unmockkAll()
        }
    }
})
