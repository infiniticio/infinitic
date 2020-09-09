package io.infinitic.taskManager.engine.pulsar.functions

import io.infinitic.taskManager.engine.avroClasses.AvroMonitoringPerName
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.infinitic.taskManager.storage.pulsar.PulsarStorage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import org.apache.pulsar.functions.api.Context
import java.util.Optional

class AvroMonitoringPerNameTests : ShouldSpec({
    context("TaskMetricsFunction.process") {

        should("throw an exception if called with a null context") {
            val function = MonitoringPerNamePulsarFunction()

            shouldThrow<NullPointerException> {
                function.process(mockk<AvroEnvelopeForMonitoringPerName>(), null)
            }
        }

        should("call metrics with correct parameters") {
            // mocking
            val topicPrefixValue = mockk<Optional<Any>>()
            every { topicPrefixValue.isPresent } returns false
            val context = mockk<Context>()
            every { context.logger } returns mockk<org.slf4j.Logger>(relaxed = true)
            every { context.getUserConfigValue("topicPrefix") } returns topicPrefixValue
            val monitoringFunction = spyk<AvroMonitoringPerName>()
            coEvery { monitoringFunction.handle(any()) } just Runs
            val avroMsg = mockk<AvroEnvelopeForMonitoringPerName>()
            // given
            val fct = MonitoringPerNamePulsarFunction()
            fct.monitoring = monitoringFunction
            // when
            fct.process(avroMsg, context)
            // then
            monitoringFunction.logger shouldBe context.logger
            (monitoringFunction.avroStorage as PulsarStorage).context shouldBe context
            coVerify(exactly = 1) { monitoringFunction.handle(avroMsg) }
        }
    }
})
