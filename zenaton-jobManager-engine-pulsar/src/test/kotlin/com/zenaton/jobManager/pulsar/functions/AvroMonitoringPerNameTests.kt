package com.zenaton.jobManager.pulsar.functions

import com.zenaton.jobManager.avroEngines.AvroMonitoringPerName
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.pulsar.dispatcher.PulsarAvroDispatcher
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
            every { monitoringFunction.handle(any()) } just Runs
            val avroMsg = mockk<AvroEnvelopeForMonitoringPerName>()
            // given
            val fct = MonitoringPerNamePulsarFunction()
            fct.monitoring = monitoringFunction
            // when
            fct.process(avroMsg, context)
            // then
            monitoringFunction.logger shouldBe context.logger
            (monitoringFunction.avroStorage as PulsarAvroStorage).context shouldBe context
            (monitoringFunction.avroDispatcher as PulsarAvroDispatcher).context shouldBe context
            verify(exactly = 1) { monitoringFunction.handle(avroMsg) }
        }
    }
})
