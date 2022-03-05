/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.tasks.metrics

import io.infinitic.common.data.ClientName
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.metrics.messages.TaskStatusUpdated
import io.infinitic.common.tasks.metrics.state.TaskMetricsState
import io.infinitic.common.tasks.metrics.storage.TaskMetricsStateStorage
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot

class MetricsPerNameTests : ShouldSpec({
    val clientName = ClientName("clientMetricsPerNameTests")

    context("TaskMetrics.handle") {
        should("should update TaskMetricsState when receiving TaskStatusUpdate message") {
            val storage = mockk<TaskMetricsStateStorage>()
            val msg = TestFactory.random(
                TaskStatusUpdated::class,
                mapOf(
                    "oldStatus" to TaskStatus.RUNNING_OK,
                    "newStatus" to TaskStatus.RUNNING_ERROR
                )
            )
            val stateIn = TestFactory.random(TaskMetricsState::class, mapOf("taskName" to msg.taskName))
            val stateOutSlot = slot<TaskMetricsState>()
            coEvery { storage.getState(msg.taskName) } returns stateIn
            coEvery { storage.putState(msg.taskName, capture(stateOutSlot)) } just runs

            val taskMetrics = TaskMetricsEngine(storage)

            taskMetrics.handle(msg)

            val stateOut = stateOutSlot.captured
            coVerifyAll {
                storage.getState(msg.taskName)
                storage.putState(msg.taskName, stateOut)
            }
            stateOut.runningErrorCount shouldBe stateIn.runningErrorCount + 1
            stateOut.runningOkCount shouldBe stateIn.runningOkCount - 1
        }

        should("dispatch message when discovering a new task type") {
            val msg = TestFactory.random(
                TaskStatusUpdated::class,
                mapOf(
                    "oldStatus" to null,
                    "newStatus" to TaskStatus.RUNNING_OK
                )
            )
            val storage = mockk<TaskMetricsStateStorage>()
            val stateOutSlot = slot<TaskMetricsState>()
            coEvery { storage.getState(msg.taskName) } returns null
            coEvery { storage.putState(msg.taskName, capture(stateOutSlot)) } just runs
            val taskMetrics = TaskMetricsEngine(storage)

            // when
            taskMetrics.handle(msg)
            // then
            val stateOut = stateOutSlot.captured
            coVerifyAll {
                storage.getState(msg.taskName)
                storage.putState(msg.taskName, stateOut)
            }
        }
    }
})