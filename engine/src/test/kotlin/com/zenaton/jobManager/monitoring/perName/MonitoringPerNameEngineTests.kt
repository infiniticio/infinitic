package com.zenaton.jobManager.monitoring.perName

import com.zenaton.commons.utils.TestFactory
import com.zenaton.jobManager.data.JobStatus
import com.zenaton.jobManager.dispatcher.Dispatcher
import com.zenaton.jobManager.logger.Logger
import com.zenaton.jobManager.monitoring.global.JobCreated
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
                JobStatusUpdated::class, mapOf(
                "oldStatus" to JobStatus.RUNNING_OK,
                "newStatus" to JobStatus.RUNNING_ERROR
            ))
            val stateIn = TestFactory.get(MonitoringPerNameState::class, mapOf("taskName" to msg.jobName))
            val stateOutSlot = slot<MonitoringPerNameState>()
            every { storage.getState(msg.jobName) } returns stateIn
            every { storage.updateState(msg.jobName, capture(stateOutSlot), any()) } just runs

            val taskMetrics = MonitoringPerNameEngine()
            taskMetrics.storage = storage
            taskMetrics.dispatcher = dispatcher
            taskMetrics.logger = logger

            taskMetrics.handle(msg)

            val stateOut = stateOutSlot.captured
            verifyAll {
                storage.getState(msg.jobName)
                storage.updateState(msg.jobName, stateOut, stateIn)
            }
            stateOut.runningErrorCount shouldBe stateIn.runningErrorCount + 1
            stateOut.runningOkCount shouldBe stateIn.runningOkCount - 1
        }

        should("dispatch message when discovering a new task type") {
            val storage = mockk<MonitoringPerNameStorage>()
            val dispatcher = mockk<Dispatcher>()
            val logger = mockk<Logger>()
            val msg = TestFactory.get(
                JobStatusUpdated::class, mapOf(
                "oldStatus" to null,
                "newStatus" to JobStatus.RUNNING_OK
            ))
            val stateOutSlot = slot<MonitoringPerNameState>()
            every { storage.getState(msg.jobName) } returns null
            every { storage.updateState(msg.jobName, capture(stateOutSlot), any()) } just runs
            every { dispatcher.dispatch(any<JobCreated>()) } just runs

            val taskMetrics = MonitoringPerNameEngine()
            taskMetrics.storage = storage
            taskMetrics.dispatcher = dispatcher
            taskMetrics.logger = logger
            // when
            taskMetrics.handle(msg)
            // then
            val stateOut = stateOutSlot.captured
            verifyAll {
                storage.getState(msg.jobName)
                storage.updateState(msg.jobName, stateOut, null)
                dispatcher.dispatch(ofType<JobCreated>())
            }
        }
    }
})
