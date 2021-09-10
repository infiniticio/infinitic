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

package io.infinitic.metrics.global.engine.storage

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.metrics.global.state.MetricsGlobalState
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot

class BinaryMetricsGlobalStateStorageTests : ShouldSpec({
    context("BinaryMetricsGlobalStateStorage.getState") {
        should("return null when state does not exist") {
            // mocking
            val storage = mockk<KeyValueStorage>()
            coEvery { storage.get(any()) } returns null
            // given
            val stateStorage = BinaryMetricsGlobalStateStorage(storage)
            // when
            val state = stateStorage.getState()
            // then
            coVerify(exactly = 1) { storage.get("metricsGlobal.state") }
            confirmVerified(storage)
            state shouldBe null
        }

        should("BinaryMetricsGlobalStateStorage: return state when state exists") {
            // mocking
            val storage = mockk<KeyValueStorage>()
            val stateIn = TestFactory.random(MetricsGlobalState::class)
            coEvery { storage.get(any()) } returns stateIn.toByteArray()
            // given
            val stateStorage = BinaryMetricsGlobalStateStorage(storage)
            // when
            val stateOut = stateStorage.getState()
            // then
            coVerify(exactly = 1) { storage.get("metricsGlobal.state") }
            confirmVerified(storage)
            stateOut shouldBe stateIn
        }
    }

    context("BinaryMetricsGlobalStateStorage.updateState") {
        should("record state") {
            // mocking
            val storage = mockk<KeyValueStorage>()
            val stateIn = TestFactory.random(MetricsGlobalState::class)
            val binSlot = slot<ByteArray>()

            coEvery { storage.put("metricsGlobal.state", capture(binSlot)) } returns Unit
            // given
            val stateStorage = BinaryMetricsGlobalStateStorage(storage)
            // when
            stateStorage.putState(stateIn)
            // then
            binSlot.isCaptured shouldBe true
            MetricsGlobalState.fromByteArray(binSlot.captured) shouldBe stateIn
        }
    }

    context("BinaryMetricsGlobalStateStorage.deleteState") {
        should("delete state") {
            // mocking
            val storage = mockk<KeyValueStorage>()
            coEvery { storage.del(any()) } returns Unit
            // given
            val stageStorage = BinaryMetricsGlobalStateStorage(storage)
            // when
            stageStorage.delState()
            // then
            coVerify(exactly = 1) { storage.del("metricsGlobal.state") }
            confirmVerified(storage)
        }
    }
})
