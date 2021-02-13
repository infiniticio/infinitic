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

package io.infinitic.tasks.engine.storage

import io.infinitic.cache.no.NoCache
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.engine.state.TaskState
import io.infinitic.tasks.engine.storage.states.TaskStateKeyValueStorage
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot
import java.nio.ByteBuffer

class TaskStateKeyValueStorageTests : ShouldSpec({
    context("TaskStateKeyValueStorage.getState") {
        should("return null when state does not exist") {
            val taskId = TestFactory.random<TaskId>()
            // mocking
            val storage = mockk<KeyValueStorage>()
            coEvery { storage.getState(any()) } returns null
            // given
            val stateStorage = TaskStateKeyValueStorage(storage, NoCache())
            // when
            val state = stateStorage.getState(taskId)
            // then
            coVerify(exactly = 1) { storage.getState("task.state.$taskId") }
            confirmVerified(storage)
            state shouldBe null
        }

        should("return state when state exists") {
            // mocking
            val storage = mockk<KeyValueStorage>()
            val stateIn = TestFactory.random<TaskState>()
            coEvery { storage.getState(any()) } returns stateIn.toByteBuffer()
            // given
            val stateStorage = TaskStateKeyValueStorage(storage, NoCache())
            // when
            val stateOut = stateStorage.getState(stateIn.taskId)
            // then
            coVerify(exactly = 1) { storage.getState("task.state.${stateIn.taskId}") }
            confirmVerified(storage)
            stateOut shouldBe stateIn
        }
    }

    context("TaskStateKeyValueStorage.updateState") {
        should("record state") {
            // mocking
            val storage = mockk<KeyValueStorage>()
            val stateIn = TestFactory.random<TaskState>()
            val binSlot = slot<ByteBuffer>()

            coEvery { storage.putState("task.state.${stateIn.taskId}", capture(binSlot)) } returns Unit
            // given
            val stateStorage = TaskStateKeyValueStorage(storage, NoCache())
            // when
            stateStorage.updateState(stateIn.taskId, stateIn, null)
            // then
            binSlot.isCaptured shouldBe true
            TaskState.fromByteBuffer(binSlot.captured) shouldBe stateIn
        }
    }

    context("TaskStateKeyValueStorage.deleteState") {
        should("delete state") {
            // mocking
            val context = mockk<KeyValueStorage>()
            val stateIn = TestFactory.random<TaskState>()
            coEvery { context.deleteState(any()) } returns Unit
            // given
            val stageStorage = TaskStateKeyValueStorage(context, NoCache())
            // when
            stageStorage.deleteState(stateIn.taskId)
            // then
            coVerify(exactly = 1) { context.deleteState("task.state.${stateIn.taskId}") }
            confirmVerified(context)
        }
    }
})
