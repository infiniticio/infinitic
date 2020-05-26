package com.zenaton.taskmanager.metrics

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.messages.metrics.TaskStatusUpdated
import com.zenaton.taskmanager.metrics.state.TaskMetricsState
import com.zenaton.taskmanager.metrics.state.TaskMetricsStateStorage
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifyAll

fun randomCount(lowerLimit: Long = 1L, upperLimit: Long = 10000L) = lowerLimit + (Math.random() * (upperLimit - lowerLimit)).toLong()

class TaskMetricsTests : ShouldSpec({
    context("TaskMetrics.handle") {
        should("increment and decrement correct counters for state transition") {
            table(
                headers("taskName", "oldStatus", "newStatus", "expectedOkChange", "expectedWarningChange", "expectedErrorChange"),
                row("MyTask", TaskStatus.RUNNING_OK, TaskStatus.RUNNING_ERROR, -1L, 0L, 1L),
                row("MyTask", TaskStatus.RUNNING_WARNING, TaskStatus.RUNNING_ERROR, 0L, -1L, 1L),
                row("MyTask", TaskStatus.RUNNING_ERROR, TaskStatus.RUNNING_WARNING, 0L, 1L, -1L)
            ).forAll { taskName, oldStatus, newStatus, expectedOkChange, expectedWarningChange, expectedErrorChange ->
                val message = TestFactory.get(TaskStatusUpdated::class, mapOf("taskName" to TaskName(taskName), "oldStatus" to oldStatus, "newStatus" to newStatus))
                val previousState = TaskMetricsState(message.taskName, randomCount(), randomCount(), randomCount(), randomCount(), randomCount())
                val initialState = previousState.copy()
                val stateStorage = mockk<TaskMetricsStateStorage>()
                every { stateStorage.incrCounter(any(), any()) } just runs
                every { stateStorage.getState(any()) } returns previousState
                every { stateStorage.getCounter(any()) } returnsMany listOf(previousState.okCount + expectedOkChange, previousState.warningCount + expectedWarningChange, previousState.errorCount + expectedErrorChange, previousState.terminatedCompletedCount, previousState.terminatedCanceledCount)
                every { stateStorage.putState(any(), any()) } just runs

                val metrics = TaskMetrics()
                metrics.stateStorage = stateStorage
                metrics.handle(message)

                val expectedState = TaskMetricsState(
                    TaskName(taskName),
                    initialState.okCount + expectedOkChange,
                    initialState.warningCount + expectedWarningChange,
                    initialState.errorCount + expectedErrorChange,
                    initialState.terminatedCompletedCount,
                    initialState.terminatedCanceledCount
                )

                verifyAll {
                    stateStorage.incrCounter("metrics.rt.counter.task.${message.taskName.name.toLowerCase()}.${oldStatus.toString().toLowerCase()}", -1L)
                    stateStorage.incrCounter("metrics.rt.counter.task.${message.taskName.name.toLowerCase()}.${newStatus.toString().toLowerCase()}", 1L)
                    stateStorage.getState("metrics.task.${message.taskName.name.toLowerCase()}.counters")
                    stateStorage.getCounter("metrics.rt.counter.task.${message.taskName.name.toLowerCase()}.running_ok")
                    stateStorage.getCounter("metrics.rt.counter.task.${message.taskName.name.toLowerCase()}.running_warning")
                    stateStorage.getCounter("metrics.rt.counter.task.${message.taskName.name.toLowerCase()}.running_error")
                    stateStorage.getCounter("metrics.rt.counter.task.${message.taskName.name.toLowerCase()}.terminated_completed")
                    stateStorage.getCounter("metrics.rt.counter.task.${message.taskName.name.toLowerCase()}.terminated_canceled")
                    stateStorage.putState("metrics.task.${message.taskName.name.toLowerCase()}.counters", expectedState)
                }
            }
        }

        should("only increment new counter when old status is null") {
            val previousState = TaskMetricsState(TaskName("OtherTask"), randomCount(), randomCount(), randomCount(), randomCount(), randomCount())
            val initialState = previousState.copy()
            val stateStorage = mockk<TaskMetricsStateStorage>()
            every { stateStorage.incrCounter(any(), any()) } just runs
            every { stateStorage.getState(any()) } returns previousState
            every { stateStorage.getCounter(any()) } returnsMany listOf(previousState.okCount + 1L, previousState.warningCount, previousState.errorCount, previousState.terminatedCompletedCount, previousState.terminatedCanceledCount)
            every { stateStorage.putState(any(), any()) } just runs

            val message = TestFactory.get(TaskStatusUpdated::class, mapOf("taskName" to TaskName("OtherTask"), "oldStatus" to null, "newStatus" to TaskStatus.RUNNING_OK))

            val metrics = TaskMetrics()
            metrics.stateStorage = stateStorage
            metrics.handle(message)

            verifyAll {
                stateStorage.incrCounter("metrics.rt.counter.task.othertask.running_ok", 1)
                stateStorage.getState("metrics.task.othertask.counters")
                stateStorage.getCounter("metrics.rt.counter.task.othertask.running_ok")
                stateStorage.getCounter("metrics.rt.counter.task.othertask.running_warning")
                stateStorage.getCounter("metrics.rt.counter.task.othertask.running_error")
                stateStorage.getCounter("metrics.rt.counter.task.othertask.terminated_completed")
                stateStorage.getCounter("metrics.rt.counter.task.othertask.terminated_canceled")
                stateStorage.putState("metrics.task.othertask.counters", TaskMetricsState(TaskName("OtherTask"), initialState.okCount + 1L, initialState.warningCount, initialState.errorCount, initialState.terminatedCompletedCount, initialState.terminatedCanceledCount))
            }
        }

        should("only decrement old counter when new status is completed") {
            val previousState = TaskMetricsState(TaskName("OtherTask"), randomCount(), randomCount(), randomCount(), randomCount(), randomCount())
            val initialState = previousState.copy()
            val stateStorage = mockk<TaskMetricsStateStorage>()
            every { stateStorage.incrCounter(any(), any()) } just runs
            every { stateStorage.getState(any()) } returns previousState
            every { stateStorage.getCounter(any()) } returnsMany listOf(initialState.okCount - 1L, initialState.warningCount, initialState.errorCount, initialState.terminatedCompletedCount + 1L, initialState.terminatedCanceledCount)
            every { stateStorage.putState(any(), any()) } just runs

            val message = TestFactory.get(TaskStatusUpdated::class, mapOf("taskName" to TaskName("OtherTask"), "oldStatus" to TaskStatus.RUNNING_OK, "newStatus" to TaskStatus.TERMINATED_COMPLETED))

            val metrics = TaskMetrics()
            metrics.stateStorage = stateStorage
            metrics.handle(message)

            verifyAll {
                stateStorage.incrCounter("metrics.rt.counter.task.othertask.running_ok", -1)
                stateStorage.incrCounter("metrics.rt.counter.task.othertask.terminated_completed", 1)
                stateStorage.getState("metrics.task.othertask.counters")
                stateStorage.getCounter("metrics.rt.counter.task.othertask.running_ok")
                stateStorage.getCounter("metrics.rt.counter.task.othertask.running_warning")
                stateStorage.getCounter("metrics.rt.counter.task.othertask.running_error")
                stateStorage.getCounter("metrics.rt.counter.task.othertask.terminated_completed")
                stateStorage.getCounter("metrics.rt.counter.task.othertask.terminated_canceled")
                stateStorage.putState("metrics.task.othertask.counters", TaskMetricsState(
                    TaskName("OtherTask"),
                    initialState.okCount - 1L,
                    initialState.warningCount,
                    initialState.errorCount,
                    initialState.terminatedCompletedCount + 1L,
                    initialState.terminatedCanceledCount
                ))
            }
        }
    }
})
