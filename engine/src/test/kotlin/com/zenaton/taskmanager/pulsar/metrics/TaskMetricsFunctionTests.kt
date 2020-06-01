package com.zenaton.taskmanager.pulsar.metrics

import com.zenaton.taskmanager.metrics.TaskMetrics
import com.zenaton.taskmanager.metrics.messages.AvroTaskMetricMessage
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
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

class TaskMetricsFunctionTests : ShouldSpec({
    context("TaskMetricsFunction.process") {

        should("throw an exception if called with a null context") {
            val function = TaskMetricsFunction()

            shouldThrow<NullPointerException> {
                function.process(mockk<AvroTaskMetricMessage>(), null)
            }
        }

        should("call metrics with correct parameters") {
            // mocking
            val context = mockk<Context>()
            every { context.logger } returns mockk<org.slf4j.Logger>()
            val taskMetrics = mockk<TaskMetrics>()
            every { taskMetrics.handle(any()) } just Runs
            every { taskMetrics.storage = any() } just Runs
            every { taskMetrics.logger = any() } just Runs
            every { taskMetrics.dispatcher = any() } just Runs
            mockkObject(TaskAvroConverter)
            every { TaskAvroConverter.fromAvro(ofType<AvroTaskMetricMessage>()) } returns mockk()

            val function = TaskMetricsFunction()
            function.taskMetrics = taskMetrics
            function.process(mockk<AvroTaskMetricMessage>(), context)

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
