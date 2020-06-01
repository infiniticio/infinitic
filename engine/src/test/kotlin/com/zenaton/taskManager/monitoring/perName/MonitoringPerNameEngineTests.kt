package com.zenaton.taskManager.monitoring.perName

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskManager.monitoring.global.TaskCreated
import com.zenaton.taskManager.data.TaskStatus
import com.zenaton.taskManager.dispatcher.Dispatcher
import com.zenaton.taskManager.logger.Logger
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verifyAll

class MonitoringPerNameEngineTests : ShouldSpec({
    context("TaskMetrics.handle") {
        should("should update TaskMetricsState when receiving TaskStatusUpdate message") {
            val storage = mockk<MonitoringPerNameStorage>()
            val dispatcher = mockk<Dispatcher>()
            val logger = mockk<Logger>()
            val msg = TestFactory.get(
                TaskStatusUpdated::class, mapOf(
                "oldStatus" to TaskStatus.RUNNING_OK,
                "newStatus" to TaskStatus.RUNNING_ERROR
            ))
            val stateIn = TestFactory.get(MonitoringPerNameState::class, mapOf("taskName" to msg.taskName))
            val stateOutSlot = slot<MonitoringPerNameState>()
            every { storage.getState(msg.taskName) } returns stateIn
            every { storage.updateState(msg.taskName, capture(stateOutSlot), any()) } just runs

            val taskMetrics = MonitoringPerNameEngine()
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
            val storage = mockk<MonitoringPerNameStorage>()
            val dispatcher = mockk<Dispatcher>()
            val logger = mockk<Logger>()
            val msg = TestFactory.get(
                TaskStatusUpdated::class, mapOf(
                "oldStatus" to null,
                "newStatus" to TaskStatus.RUNNING_OK
            ))
            val stateOutSlot = slot<MonitoringPerNameState>()
            every { storage.getState(msg.taskName) } returns null
            every { storage.updateState(msg.taskName, capture(stateOutSlot), any()) } just runs
            every { dispatcher.dispatch(any<TaskCreated>()) } just runs

            val taskMetrics = MonitoringPerNameEngine()
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
                dispatcher.dispatch(ofType<TaskCreated>())
            }
        }
    }
})
