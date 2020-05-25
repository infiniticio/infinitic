package com.zenaton.taskmanager.metrics

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.messages.metrics.TaskStatusUpdated
import com.zenaton.taskmanager.pulsar.state.PulsarTaskEngineStateStorage
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verifyAll
import org.apache.pulsar.functions.api.Context

class TaskMetricsTests : ShouldSpec({
    context("TaskMetrics.handle") {
        should("increment and decrement correct counters for state transition") {
            table(
                headers("message", "decrementedCounter", "incrementedCounter"),
                row(
                    TestFactory.get(TaskStatusUpdated::class, mapOf("taskName" to TaskName("MyTask"), "oldStatus" to TaskStatus.OK, "newStatus" to TaskStatus.ERROR)),
                    "metrics.rt.counter.task.mytask.ok",
                    "metrics.rt.counter.task.mytask.error"
                ),
                row(
                    TestFactory.get(TaskStatusUpdated::class, mapOf("taskName" to TaskName("MyTask"), "oldStatus" to TaskStatus.WARNING, "newStatus" to TaskStatus.ERROR)),
                    "metrics.rt.counter.task.mytask.warning",
                    "metrics.rt.counter.task.mytask.error"
                ),
                row(
                    TestFactory.get(TaskStatusUpdated::class, mapOf("taskName" to TaskName("OtherTask"), "oldStatus" to TaskStatus.ERROR, "newStatus" to TaskStatus.WARNING)),
                    "metrics.rt.counter.task.othertask.error",
                    "metrics.rt.counter.task.othertask.warning"
                )
            ).forAll { taskStatusUpdated, decrementedCounter, incrementedCounter ->
                val context = mockk<Context>()
                every { context.incrCounter(any(), any()) } just Runs

                val metrics = TaskMetrics()
                metrics.stateStorage = PulsarTaskEngineStateStorage(context)
                metrics.handle(taskStatusUpdated)

                verifyAll {
                    context.incrCounter(decrementedCounter, -1)
                    context.incrCounter(incrementedCounter, 1)
                }
            }
        }
    }
})
