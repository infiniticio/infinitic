// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.engines.monitoringPerName.storage

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.serDe.avro.AvroConverter
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.monitoringPerName.state.MonitoringPerNameState
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyAll
import java.nio.ByteBuffer

class MonitoringPerNameStateKeyValueStorageTests : ShouldSpec({
    context("MonitoringPerNameStateKeyValueStorage.getState") {

        should("return null when state does not exist") {
            // given
            val taskName = TaskName(TestFactory.random(String::class))
            val context = mockk<KeyValueStorage>()
            every { context.getState(any()) } returns null
            // when
            val stateStorage = MonitoringPerNameStateKeyValueStorage(context)
            val state = stateStorage.getState(taskName)
            // then
            verify(exactly = 1) { context.getState("monitoringPerName.state.$taskName") }
            confirmVerified(context)
            state shouldBe null
        }

        should("return state when state exists") {
            // given
            val stateIn = TestFactory.random(MonitoringPerNameState::class)
            val context = mockk<KeyValueStorage>()
            every { context.getState(any()) } returns ByteBuffer.wrap(stateIn.toByteArray())
            // when
            val stateStorage = MonitoringPerNameStateKeyValueStorage(context)
            val stateOut = stateStorage.getState(stateIn.taskName)
            // then
            verify(exactly = 1) { context.getState("monitoringPerName.state.${stateIn.taskName}") }
            confirmVerified(context)
            stateOut shouldBe stateIn
        }
    }

    context("MonitoringPerNameStateKeyValueStorage.updateState") {

        should("initializes all counters when old state is null and save state") {
            val storage = mockk<KeyValueStorage>()
            val newState = TestFactory.random(
                MonitoringPerNameState::class,
                mapOf(
                    "runningOkCount" to 1L,
                    "runningWarningCount" to 0L,
                    "runningErrorCount" to 0L,
                    "terminatedCompletedCount" to 0L,
                    "terminatedCanceledCount" to 0L
                )
            )
            every { storage.incrementCounter(any(), any()) } just runs
            every { storage.getState(any()) } returns mockk()
            every { storage.getCounter(any()) } returnsMany listOf(14L, 2L, 1L, 30L, 100L)
            every { storage.putState(any(), any()) } just runs

            mockkObject(AvroConverter)

            val stateStorage = MonitoringPerNameStateKeyValueStorage(storage)
            stateStorage.updateState(newState.taskName, newState, null)

            verifyAll {
                storage.incrementCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_OK), newState.runningOkCount)
                storage.incrementCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_WARNING), newState.runningWarningCount)
                storage.incrementCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_ERROR), newState.runningErrorCount)
                storage.incrementCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_COMPLETED), newState.terminatedCompletedCount)
                storage.incrementCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_CANCELED), newState.terminatedCanceledCount)
                storage.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_OK))
                storage.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_WARNING))
                storage.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_ERROR))
                storage.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_CANCELED))
                storage.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_COMPLETED))
                storage.putState(stateStorage.getMonitoringPerNameStateKey(newState.taskName), ofType())
            }

            unmockkAll()
        }

        should("increment and decrement counters accordingly") {
            val storage = mockk<KeyValueStorage>()
            val oldState = TestFactory.random(
                MonitoringPerNameState::class,
                mapOf(
                    "runningOkCount" to 10L,
                    "runningWarningCount" to 17L,
                    "terminatedCompletedCount" to 22L
                )
            )
            val newState = TestFactory.random(
                MonitoringPerNameState::class,
                mapOf(
                    "runningOkCount" to 10L,
                    "runningWarningCount" to 17L,
                    "terminatedCompletedCount" to 22L
                )
            )
            every { storage.incrementCounter(any(), any()) } just runs
            every { storage.getState(any()) } returns mockk()
            every { storage.getCounter(any()) } returnsMany listOf(14L, 2L, 1L, 30L, 100L)
            every { storage.putState(any(), any()) } just runs

            mockkObject(AvroConverter)

            val stateStorage = MonitoringPerNameStateKeyValueStorage(storage)
            stateStorage.updateState(newState.taskName, newState, oldState)

            verifyAll {
                storage.incrementCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_ERROR), newState.runningErrorCount - oldState.runningErrorCount)
                storage.incrementCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_CANCELED), newState.terminatedCanceledCount - oldState.terminatedCanceledCount)
                storage.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_OK))
                storage.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_WARNING))
                storage.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_ERROR))
                storage.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_CANCELED))
                storage.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_COMPLETED))
                storage.putState(stateStorage.getMonitoringPerNameStateKey(newState.taskName), ofType())
            }

            unmockkAll()
        }
    }

    context("MonitoringPerNameStateKeyValueStorage.deleteState") {
        should("should delete state") {
            // given
            val stateIn = TestFactory.random(MonitoringPerNameState::class)
            val storage = mockk<KeyValueStorage>()
            every { storage.deleteState(any()) } returns Unit
            // when
            val stateStorage = MonitoringPerNameStateKeyValueStorage(storage)
            stateStorage.deleteState(stateIn.taskName)
            // then
            verify(exactly = 1) { storage.deleteState(stateStorage.getMonitoringPerNameStateKey(stateIn.taskName)) }
            confirmVerified(storage)
        }
    }
})
