package com.zenaton.taskmanager.pulsar.metrics

import com.zenaton.taskmanager.messages.metrics.AvroTaskMetricMessage
import com.zenaton.taskmanager.metrics.TaskMetrics
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
    context("TaskMetricsFunction.handle") {
        should("throw an exception if called with a null context") {
            val function = TaskMetricsFunction()

            shouldThrow<NullPointerException> {
                function.process(mockk(), null)
            }
        }

        should("call metrics with correct parameters") {
            val taskMetrics = mockk<TaskMetrics>()
            every { taskMetrics.handle(any()) } just Runs
            every { taskMetrics.stateStorage = any() } just Runs

            mockkObject(TaskAvroConverter)
            every { TaskAvroConverter.fromAvro(ofType<AvroTaskMetricMessage>()) } returns mockk()

            val function = TaskMetricsFunction()
            function.taskMetrics = taskMetrics
            function.process(mockk(), mockk())

            verifyAll {
                taskMetrics.stateStorage = ofType()
                taskMetrics.handle(ofType())
            }

            unmockkAll()
        }
    }
})
