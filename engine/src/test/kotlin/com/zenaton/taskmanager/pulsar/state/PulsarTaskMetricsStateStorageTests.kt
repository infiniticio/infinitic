package com.zenaton.taskmanager.pulsar.state

import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.metrics.state.TaskMetricsState
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verifyAll
import org.apache.pulsar.functions.api.Context

class PulsarTaskMetricsStateStorageTests : ShouldSpec({
    context("PulsarTaskMetricsStateStorage.updateTaskStatusCountersByName") {
        should("only increment new counter when old status is null and save state") {
            val context = mockk<Context>()
            every { context.incrCounter(any(), any()) } just runs
            every { context.getCounter(any()) } returnsMany listOf(14L, 2L, 1L, 30L, 100L)
            every { context.putState(any(), any()) } just runs

            mockkObject(TaskAvroConverter)

            val stateStorage = PulsarTaskMetricsStateStorage(context)
            stateStorage.updateTaskStatusCountersByName(TaskName("SomeTask"), null, TaskStatus.RUNNING_OK)

            verifyAll {
                context.incrCounter("metrics.rt.counter.task.sometask.running_ok", 1L)
                context.getCounter("metrics.rt.counter.task.sometask.running_ok")
                context.getCounter("metrics.rt.counter.task.sometask.running_warning")
                context.getCounter("metrics.rt.counter.task.sometask.running_error")
                context.getCounter("metrics.rt.counter.task.sometask.terminated_completed")
                context.getCounter("metrics.rt.counter.task.sometask.terminated_canceled")
                TaskAvroConverter.toAvro(TaskMetricsState(TaskName("SomeTask"), 14L, 2L, 1L, 30L, 100L))
                context.putState("metrics.task.sometask.counters", ofType())
            }

            unmockkAll()
        }

        should("increment and decrement correct counters for state transition") {
            table(
                headers("taskName", "oldStatus", "newStatus"),
                row("MyTask", TaskStatus.RUNNING_OK, TaskStatus.RUNNING_ERROR),
                row("MyTask", TaskStatus.RUNNING_WARNING, TaskStatus.RUNNING_ERROR),
                row("MyTask", TaskStatus.RUNNING_ERROR, TaskStatus.RUNNING_WARNING),
                row("MyTask", TaskStatus.RUNNING_OK, TaskStatus.TERMINATED_COMPLETED),
                row("MyTask", TaskStatus.RUNNING_WARNING, TaskStatus.TERMINATED_COMPLETED),
                row("MyTask", TaskStatus.RUNNING_ERROR, TaskStatus.TERMINATED_CANCELED)
            ).forAll { taskName, oldStatus, newStatus ->
                val context = mockk<Context>()
                every { context.incrCounter(any(), any()) } just runs
                every { context.getCounter(any()) } returnsMany listOf(28L, 3L, 6L, 30L, 100L)
                every { context.putState(any(), any()) } just runs

                mockkObject(TaskAvroConverter)

                val stateStorage = PulsarTaskMetricsStateStorage(context)
                stateStorage.updateTaskStatusCountersByName(TaskName(taskName), oldStatus, newStatus)

                verifyAll {
                    context.incrCounter("metrics.rt.counter.task.${taskName.toLowerCase()}.${oldStatus.toString().toLowerCase()}", -1L)
                    context.incrCounter("metrics.rt.counter.task.${taskName.toLowerCase()}.${newStatus.toString().toLowerCase()}", 1L)
                    context.getCounter("metrics.rt.counter.task.${taskName.toLowerCase()}.running_ok")
                    context.getCounter("metrics.rt.counter.task.${taskName.toLowerCase()}.running_warning")
                    context.getCounter("metrics.rt.counter.task.${taskName.toLowerCase()}.running_error")
                    context.getCounter("metrics.rt.counter.task.${taskName.toLowerCase()}.terminated_completed")
                    context.getCounter("metrics.rt.counter.task.${taskName.toLowerCase()}.terminated_canceled")
                    TaskAvroConverter.toAvro(TaskMetricsState(TaskName(taskName), 28L, 3L, 6L, 30L, 100L))
                    context.putState("metrics.task.${taskName.toLowerCase()}.counters", ofType())
                }

                unmockkAll()
            }
        }
    }
})
