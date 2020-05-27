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
                    TestFactory.get(TaskStatusUpdated::class, mapOf("taskName" to TaskName("MyTask"), "oldStatus" to TaskStatus.RUNNING_OK, "newStatus" to TaskStatus.RUNNING_ERROR)),
                    "metrics.rt.counter.task.mytask.running_ok",
                    "metrics.rt.counter.task.mytask.running_error"
                ),
                row(
                    TestFactory.get(TaskStatusUpdated::class, mapOf("taskName" to TaskName("MyTask"), "oldStatus" to TaskStatus.RUNNING_WARNING, "newStatus" to TaskStatus.RUNNING_ERROR)),
                    "metrics.rt.counter.task.mytask.running_warning",
                    "metrics.rt.counter.task.mytask.running_error"
                ),
                row(
                    TestFactory.get(TaskStatusUpdated::class, mapOf("taskName" to TaskName("OtherTask"), "oldStatus" to TaskStatus.RUNNING_ERROR, "newStatus" to TaskStatus.RUNNING_WARNING)),
                    "metrics.rt.counter.task.othertask.running_error",
                    "metrics.rt.counter.task.othertask.running_warning"
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

            val message = TestFactory.get(TaskStatusUpdated::class, mapOf("taskName" to TaskName("OtherTask"), "oldStatus" to null, "newStatus" to TaskStatus.RUNNING_OK))

            val metrics = TaskMetrics()
            metrics.stateStorage = stateStorage
            metrics.handle(message)

            verifyAll {
                stateStorage.incrCounter("metrics.rt.counter.task.othertask.running_ok", 1)
            }
        }

        should("only decrement old counter when new status is completed") {
            val stateStorage = mockk<StateStorage>()
            every { stateStorage.incrCounter(any(), any()) } just Runs

            val message = TestFactory.get(TaskStatusUpdated::class, mapOf("taskName" to TaskName("OtherTask"), "oldStatus" to TaskStatus.RUNNING_OK, "newStatus" to TaskStatus.TERMINATED_COMPLETED))

            val metrics = TaskMetrics()
            metrics.stateStorage = stateStorage
            metrics.handle(message)

            verifyAll {
                stateStorage.incrCounter("metrics.rt.counter.task.othertask.running_ok", -1)
                stateStorage.incrCounter("metrics.rt.counter.task.othertask.terminated_completed", 1)
            }
        }
    }
})
