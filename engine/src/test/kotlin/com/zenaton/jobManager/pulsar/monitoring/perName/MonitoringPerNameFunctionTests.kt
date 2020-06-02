package com.zenaton.jobManager.pulsar.monitoring.perName

import com.zenaton.jobManager.messages.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.monitoring.perName.MonitoringPerNameEngine
import com.zenaton.jobManager.pulsar.avro.AvroConverter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verifyAll
import org.apache.pulsar.functions.api.Context

class MonitoringPerNameFunctionTests : ShouldSpec({
    context("TaskMetricsFunction.process") {

        should("throw an exception if called with a null context") {
            val function = MonitoringPerNameFunction()

            shouldThrow<NullPointerException> {
                function.process(mockk<AvroForMonitoringPerNameMessage>(), null)
            }
        }

        should("call metrics with correct parameters") {
            // mocking
            val context = mockk<Context>()
            every { context.logger } returns mockk<org.slf4j.Logger>()
            val taskMetrics = mockk<MonitoringPerNameEngine>()
            every { taskMetrics.handle(any()) } just Runs
            every { taskMetrics.storage = any() } just Runs
            every { taskMetrics.logger = any() } just Runs
            every { taskMetrics.dispatcher = any() } just Runs
            mockkObject(AvroConverter)
            every { AvroConverter.fromAvro(ofType<AvroForMonitoringPerNameMessage>()) } returns mockk()

            val function = MonitoringPerNameFunction()
            function.taskMetrics = taskMetrics
            function.process(mockk<AvroForMonitoringPerNameMessage>(), context)

            verifyAll {
                taskMetrics.storage = ofType()
                taskMetrics.logger = ofType()
                taskMetrics.dispatcher = ofType()
                taskMetrics.handle(ofType())
            }

            unmockkAll()
        }
    }
})
