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

package io.infinitic.engines.monitoringGlobal.storage

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.monitoringGlobal.state.MonitoringGlobalState
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.nio.ByteBuffer

class MonitoringGlobalKeyValueStorageTests : ShouldSpec({
    context("MonitoringGlobalStateKeyValueStorage.getState") {
        should("return null when state does not exist") {
            // mocking
            val storage = mockk<KeyValueStorage>()
            every { storage.getState(any()) } returns null
            // given
            val stateStorage = MonitoringGlobalStateKeyValueStorage(storage)
            // when
            val state = stateStorage.getState()
            // then
            verify(exactly = 1) { storage.getState("monitoringGlobal.state") }
            confirmVerified(storage)
            state shouldBe null
        }

        should("MonitoringGlobalStateKeyValueStorage: return state when state exists") {
            // mocking
            val storage = mockk<KeyValueStorage>()
            val stateIn = TestFactory.random(MonitoringGlobalState::class)
            every { storage.getState(any()) } returns ByteBuffer.wrap(stateIn.toByteArray())
            // given
            val stateStorage = MonitoringGlobalStateKeyValueStorage(storage)
            // when
            val stateOut = stateStorage.getState()
            // then
            verify(exactly = 1) { storage.getState("monitoringGlobal.state") }
            confirmVerified(storage)
            stateOut shouldBe stateIn
        }
    }

    context("MonitoringGlobalStateKeyValueStorage.updateState") {
        should("record state") {
            // mocking
            val storage = mockk<KeyValueStorage>()
            val stateIn = TestFactory.random(MonitoringGlobalState::class)
            val binSlot = slot<ByteBuffer>()

            every { storage.putState("monitoringGlobal.state", capture(binSlot)) } returns Unit
            // given
            val stateStorage = MonitoringGlobalStateKeyValueStorage(storage)
            // when
            stateStorage.updateState(stateIn, null)
            // then
            binSlot.isCaptured shouldBe true
            MonitoringGlobalState.fromByteBuffer(binSlot.captured) shouldBe stateIn
        }
    }

    context("MonitoringGlobalStateKeyValueStorage.deleteState") {
        should("delete state") {
            // mocking
            val storage = mockk<KeyValueStorage>()
            every { storage.deleteState(any()) } returns Unit
            // given
            val stageStorage = MonitoringGlobalStateKeyValueStorage(storage)
            // when
            stageStorage.deleteState()
            // then
            verify(exactly = 1) { storage.deleteState("monitoringGlobal.state") }
            confirmVerified(storage)
        }
    }
})
