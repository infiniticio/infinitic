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

package io.infinitic.monitoring.perName.engine.storage

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.metrics.perName.state.MetricsPerNameState
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.tasks.data.TaskName
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot

class BinaryMonitoringPerNameStateStorageTests : ShouldSpec({
    context("BinaryMonitoringPerNameStateStorage.getState") {

        should("return null when state does not exist") {
            // given
            val taskName = TaskName(TestFactory.random(String::class))
            val storage = mockk<KeyValueStorage>()
            coEvery { storage.getValue(any()) } returns null
            // when
            val stateStorage = BinaryMonitoringPerNameStateStorage(storage)
            val state = stateStorage.getState(taskName)
            // then
            coVerify(exactly = 1) { storage.getValue("monitoringPerName.state.$taskName") }
            confirmVerified(storage)
            state shouldBe null
        }

        should("return state when state exists") {
            // given
            val stateIn = TestFactory.random<MetricsPerNameState>()
            val storage = mockk<KeyValueStorage>()
            coEvery { storage.getValue(any()) } returns stateIn.toByteArray()
            // when
            val stateStorage = BinaryMonitoringPerNameStateStorage(storage)
            val stateOut = stateStorage.getState(stateIn.taskName)
            // then
            coVerify(exactly = 1) { storage.getValue("monitoringPerName.state.${stateIn.taskName}") }
            confirmVerified(storage)
            stateOut shouldBe stateIn
        }
    }

    context("BinaryMonitoringPerNameStateStorage.putState") {

        should("update state") {
            // given
            val state = TestFactory.random<MetricsPerNameState>()
            val storage = mockk<KeyValueStorage>()
            val binSlot = slot<ByteArray>()
            coEvery { storage.putValue("monitoringPerName.state.${state.taskName}", capture(binSlot)) } returns Unit
            // when
            val stateStorage = BinaryMonitoringPerNameStateStorage(storage)
            stateStorage.putState(state.taskName, state)
            // then
            binSlot.isCaptured shouldBe true
            MetricsPerNameState.fromByteArray(binSlot.captured) shouldBe state
        }
    }

    context("BinaryMonitoringPerNameStateStorage.delState") {
        should("delete state") {
            // given
            val state = TestFactory.random(MetricsPerNameState::class)
            val storage = mockk<KeyValueStorage>()
            coEvery { storage.delValue(any()) } just runs
            // when
            val stateStorage = BinaryMonitoringPerNameStateStorage(storage)
            stateStorage.delState(state.taskName)
            // then
            coVerify(exactly = 1) { storage.delValue("monitoringPerName.state.${state.taskName}") }
            confirmVerified(storage)
        }
    }
})
