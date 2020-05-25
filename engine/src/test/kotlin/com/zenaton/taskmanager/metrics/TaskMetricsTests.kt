package com.zenaton.taskmanager.metrics

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.messages.metrics.TaskStatusUpdated
import com.zenaton.taskmanager.state.StateStorage
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verifyAll

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
                val stateStorage = mockk<StateStorage>()
                every { stateStorage.incrCounter(any(), any()) } just Runs

                val metrics = TaskMetrics()
                metrics.stateStorage = stateStorage
                metrics.handle(taskStatusUpdated)

                verifyAll {
                    stateStorage.incrCounter(decrementedCounter, -1)
                    stateStorage.incrCounter(incrementedCounter, 1)
                }
            }
        }

        should("only increment new counter when old status is null") {
            val stateStorage = mockk<StateStorage>()
            every { stateStorage.incrCounter(any(), any()) } just Runs

            val message = TestFactory.get(TaskStatusUpdated::class, mapOf("taskName" to TaskName("OtherTask"), "oldStatus" to null, "newStatus" to TaskStatus.OK))

            val metrics = TaskMetrics()
            metrics.stateStorage = stateStorage
            metrics.handle(message)

            verifyAll {
                stateStorage.incrCounter("metrics.rt.counter.task.othertask.ok", 1)
            }
        }

        should("only decrement old counter when new status is null") {
            val stateStorage = mockk<StateStorage>()
            every { stateStorage.incrCounter(any(), any()) } just Runs

            val message = TestFactory.get(TaskStatusUpdated::class, mapOf("taskName" to TaskName("OtherTask"), "oldStatus" to TaskStatus.OK, "newStatus" to null))

            val metrics = TaskMetrics()
            metrics.stateStorage = stateStorage
            metrics.handle(message)

            verifyAll {
                stateStorage.incrCounter("metrics.rt.counter.task.othertask.ok", -1)
            }
        }

        should("do nothing when both statuses are null") {
            val stateStorage = mockk<StateStorage>()

            val message = TestFactory.get(TaskStatusUpdated::class, mapOf("taskName" to TaskName("OtherTask"), "oldStatus" to null, "newStatus" to null))

            val metrics = TaskMetrics()
            metrics.stateStorage = stateStorage
            metrics.handle(message)

            confirmVerified(stateStorage)
        }
    }
})
