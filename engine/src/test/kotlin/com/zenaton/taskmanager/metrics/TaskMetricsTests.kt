package com.zenaton.taskmanager.metrics

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskmanager.admin.messages.TaskTypeCreated
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.dispatcher.TaskDispatcher
import com.zenaton.taskmanager.logger.TaskLogger
import com.zenaton.taskmanager.metrics.messages.TaskStatusUpdated
import com.zenaton.taskmanager.metrics.state.TaskMetricsState
import com.zenaton.taskmanager.metrics.state.TaskMetricsStateStorage
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verifyAll

class TaskMetricsTests : ShouldSpec({
    context("TaskMetrics.handle") {
        should("should update TaskMetricsState when receiving TaskStatusUpdate message") {
            val storage = mockk<TaskMetricsStateStorage>()
            val dispatcher = mockk<TaskDispatcher>()
            val logger = mockk<TaskLogger>()
            val msg = TestFactory.get(TaskStatusUpdated::class, mapOf(
                "oldStatus" to TaskStatus.RUNNING_OK,
                "newStatus" to TaskStatus.RUNNING_ERROR
            ))
            val stateIn = TestFactory.get(TaskMetricsState::class, mapOf("taskName" to msg.taskName))
            val stateOutSlot = slot<TaskMetricsState>()
            every { storage.getState(msg.taskName) } returns stateIn
            every { storage.updateState(msg.taskName, capture(stateOutSlot), any()) } just runs

            val taskMetrics = TaskMetrics()
            taskMetrics.storage = storage
            taskMetrics.dispatcher = dispatcher
            taskMetrics.logger = logger

            taskMetrics.handle(msg)

            val stateOut = stateOutSlot.captured
            verifyAll {
                storage.getState(msg.taskName)
                storage.updateState(msg.taskName, stateOut, stateIn)
            }
            stateOut.runningErrorCount shouldBe stateIn.runningErrorCount + 1
            stateOut.runningOkCount shouldBe stateIn.runningOkCount - 1
        }

        should("dispatch message when discovering a new task type") {
            val storage = mockk<TaskMetricsStateStorage>()
            val dispatcher = mockk<TaskDispatcher>()
            val logger = mockk<TaskLogger>()
            val msg = TestFactory.get(TaskStatusUpdated::class, mapOf(
                "oldStatus" to null,
                "newStatus" to TaskStatus.RUNNING_OK
            ))
            val stateOutSlot = slot<TaskMetricsState>()
            every { storage.getState(msg.taskName) } returns null
            every { storage.updateState(msg.taskName, capture(stateOutSlot), any()) } just runs
            every { dispatcher.dispatch(any<TaskTypeCreated>()) } just runs

            val taskMetrics = TaskMetrics()
            taskMetrics.storage = storage
            taskMetrics.dispatcher = dispatcher
            taskMetrics.logger = logger
            // when
            taskMetrics.handle(msg)
            // then
            val stateOut = stateOutSlot.captured
            verifyAll {
                storage.getState(msg.taskName)
                storage.updateState(msg.taskName, stateOut, null)
                dispatcher.dispatch(ofType<TaskTypeCreated>())
            }
        }
    }
})
