package io.infinitic.taskManager.engine.pulsar.functions

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.engine.engines.MonitoringPerName
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
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
import java.util.Optional

class MonitoringPerNamePulsarFunctionTests : ShouldSpec({
    context("MonitoringPerNamePulsarFunctionTests.process") {

        should("throw an exception if called with a null context") {
            val function = MonitoringPerNamePulsarFunction()

            shouldThrow<NullPointerException> {
                function.process(mockk(), null)
            }
        }

        should("call metrics with correct parameters") {
            // mocking
            val topicPrefixValue = mockk<Optional<Any>>()
            every { topicPrefixValue.isPresent } returns false
            val context = mockk<Context>()
            every { context.logger } returns mockk(relaxed = true)
            every { context.getUserConfigValue("topicPrefix") } returns topicPrefixValue
            val monitoringFunction = spyk<MonitoringPerName>()
            coEvery { monitoringFunction.handle(any()) } just Runs
            val avroMsg = mockk<AvroEnvelopeForMonitoringPerName>()
            mockkObject(AvroConverter)
            every { AvroConverter.fromMonitoringPerName(any()) } returns mockk()
            // given
            val fct = MonitoringPerNamePulsarFunction()
            fct.monitoring = monitoringFunction
            // when
            fct.process(avroMsg, context)
            // then
            monitoringFunction.logger shouldBe context.logger
            coVerify(exactly = 1) { monitoringFunction.handle(any()) }
            unmockkAll()
        }
    }
})
