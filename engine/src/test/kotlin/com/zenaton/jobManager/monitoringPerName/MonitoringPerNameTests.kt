package com.zenaton.jobManager.monitoringPerName

import com.zenaton.jobManager.data.JobStatus
import com.zenaton.jobManager.dispatcher.Dispatcher
import com.zenaton.jobManager.messages.JobCreated
import com.zenaton.jobManager.messages.JobStatusUpdated
import com.zenaton.jobManager.utils.TestFactory
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verifyAll
import org.slf4j.Logger

class MonitoringPerNameTests : ShouldSpec({
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

            val monitoringPerName = MonitoringPerName()
            monitoringPerName.logger = logger
            monitoringPerName.storage = storage
            monitoringPerName.dispatcher = dispatcher

            monitoringPerName.handle(msg)

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
            every { dispatcher.toMonitoringGlobal(any<JobCreated>()) } just runs

            val monitoringPerName = MonitoringPerName()
            monitoringPerName.logger = logger
            monitoringPerName.storage = storage
            monitoringPerName.dispatcher = dispatcher
            // when
            monitoringPerName.handle(msg)
            // then
            val stateOut = stateOutSlot.captured
            verifyAll {
                storage.getState(msg.jobName)
                storage.updateState(msg.jobName, stateOut, null)
                dispatcher.toMonitoringGlobal(ofType<JobCreated>())
            }
        }
    }
})
