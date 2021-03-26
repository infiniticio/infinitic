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

package io.infinitic.monitoring.global.engine.storage

import io.infinitic.cache.no.NoCache
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.monitoring.global.state.MonitoringGlobalState
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot

class MonitoringGlobalKeyValueStorageTests : ShouldSpec({
    context("MonitoringGlobalStateKeyValueStorage.getState") {
        should("return null when state does not exist") {
            // mocking
            val storage = mockk<KeyValueStorage>()
            coEvery { storage.getValue(any()) } returns null
            // given
            val stateStorage = MonitoringGlobalStateKeyValueStorage(storage, NoCache())
            // when
            val state = stateStorage.getState()
            // then
            coVerify(exactly = 1) { storage.getValue("monitoringGlobal.state") }
            confirmVerified(storage)
            state shouldBe null
        }

        should("MonitoringGlobalStateKeyValueStorage: return state when state exists") {
            // mocking
            val storage = mockk<KeyValueStorage>()
            val stateIn = TestFactory.random(MonitoringGlobalState::class)
            coEvery { storage.getValue(any()) } returns stateIn.toByteArray()
            // given
            val stateStorage = MonitoringGlobalStateKeyValueStorage(storage, NoCache())
            // when
            val stateOut = stateStorage.getState()
            // then
            coVerify(exactly = 1) { storage.getValue("monitoringGlobal.state") }
            confirmVerified(storage)
            stateOut shouldBe stateIn
        }
    }

    context("MonitoringGlobalStateKeyValueStorage.updateState") {
        should("record state") {
            // mocking
            val storage = mockk<KeyValueStorage>()
            val stateIn = TestFactory.random(MonitoringGlobalState::class)
            val binSlot = slot<ByteArray>()

            coEvery { storage.putValue("monitoringGlobal.state", capture(binSlot)) } returns Unit
            // given
            val stateStorage = MonitoringGlobalStateKeyValueStorage(storage, NoCache())
            // when
            stateStorage.putState(stateIn)
            // then
            binSlot.isCaptured shouldBe true
            MonitoringGlobalState.fromByteArray(binSlot.captured) shouldBe stateIn
        }
    }

    context("MonitoringGlobalStateKeyValueStorage.deleteState") {
        should("delete state") {
            // mocking
            val storage = mockk<KeyValueStorage>()
            coEvery { storage.delValue(any()) } returns Unit
            // given
            val stageStorage = MonitoringGlobalStateKeyValueStorage(storage, NoCache())
            // when
            stageStorage.delState()
            // then
            coVerify(exactly = 1) { storage.delValue("monitoringGlobal.state") }
            confirmVerified(storage)
        }
    }
})
