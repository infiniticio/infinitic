package io.infinitic.taskManager.engine.engines

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.taskManager.data.TaskStatus
import io.infinitic.common.taskManager.messages.TaskCreated
import io.infinitic.common.taskManager.messages.TaskStatusUpdated
import io.infinitic.common.taskManager.states.MonitoringPerNameState
import io.infinitic.taskManager.engine.storage.TaskStateStorage
import io.infinitic.taskManager.engine.utils.TestFactory
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verifyAll

class MonitoringPerNameTests : ShouldSpec({
    context("TaskMetrics.handle") {
        should("should update TaskMetricsState when receiving TaskStatusUpdate message") {
            val storage = mockk<TaskStateStorage>()
            val dispatcher = mockk<Dispatcher>()
            val msg = TestFactory.random(
                TaskStatusUpdated::class,
                mapOf(
                    "oldStatus" to TaskStatus.RUNNING_OK,
                    "newStatus" to TaskStatus.RUNNING_ERROR
                )
            )
            val stateIn = TestFactory.random(MonitoringPerNameState::class, mapOf("taskName" to msg.taskName))
            val stateOutSlot = slot<MonitoringPerNameState>()
            every { storage.getMonitoringPerNameState(msg.taskName) } returns stateIn
            every { storage.updateMonitoringPerNameState(msg.taskName, capture(stateOutSlot), any()) } just runs

            val monitoringPerName = MonitoringPerName(storage, dispatcher)

            monitoringPerName.handle(msg)

            val stateOut = stateOutSlot.captured
            verifyAll {
                storage.getMonitoringPerNameState(msg.taskName)
                storage.updateMonitoringPerNameState(msg.taskName, stateOut, stateIn)
            }
            stateOut.runningErrorCount shouldBe stateIn.runningErrorCount + 1
            stateOut.runningOkCount shouldBe stateIn.runningOkCount - 1
        }

        should("dispatch message when discovering a new task type") {
            val storage = mockk<TaskStateStorage>()
            val dispatcher = mockk<Dispatcher>()
            val msg = TestFactory.random(
                TaskStatusUpdated::class,
                mapOf(
                    "oldStatus" to null,
                    "newStatus" to TaskStatus.RUNNING_OK
                )
            )
            val stateOutSlot = slot<MonitoringPerNameState>()
            every { storage.getMonitoringPerNameState(msg.taskName) } returns null
            every { storage.updateMonitoringPerNameState(msg.taskName, capture(stateOutSlot), any()) } just runs
            coEvery { dispatcher.toMonitoringGlobal(any<TaskCreated>()) } just runs

            val monitoringPerName = MonitoringPerName(storage, dispatcher)

            // when
            monitoringPerName.handle(msg)
            // then
            val stateOut = stateOutSlot.captured
            coVerifyAll {
                storage.getMonitoringPerNameState(msg.taskName)
                storage.updateMonitoringPerNameState(msg.taskName, stateOut, null)
                dispatcher.toMonitoringGlobal(ofType<TaskCreated>())
            }
        }
    }
})
