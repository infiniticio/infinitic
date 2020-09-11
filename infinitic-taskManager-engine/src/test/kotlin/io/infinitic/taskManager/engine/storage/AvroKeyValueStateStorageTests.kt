package io.infinitic.taskManager.engine.storage

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.storage.api.Storage
import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.taskManager.common.data.TaskStatus
import io.infinitic.taskManager.common.states.MonitoringGlobalState
import io.infinitic.taskManager.common.states.MonitoringPerNameState
import io.infinitic.taskManager.common.states.State
import io.infinitic.taskManager.common.states.TaskEngineState
import io.infinitic.taskManager.engine.utils.TestFactory
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

class AvroKeyValueStateStorageTests : ShouldSpec({
    context("AvroKeyValueStateStorage.getEngineState") {
        should("return null when state does not exist") {
            val taskId = TaskId(TestFactory.random(String::class))
            // mocking
            val storage = mockk<Storage>()
            every { storage.getState(any()) } returns null
            // given
            val stateStorage = AvroKeyValueStateStorage(storage)
            // when
            val state = stateStorage.getTaskEngineState(taskId)
            // then
            verify(exactly = 1) { storage.getState("engine.state.${taskId.id}") }
            confirmVerified(storage)
            state shouldBe null
        }

        should("return state when state exists") {
            // mocking
            val context = mockk<Storage>()
            val stateIn = TestFactory.random(TaskEngineState::class)
            every { context.getState(any()) } returns byteBufferRepresentation(stateIn)
            // given
            val stateStorage = AvroKeyValueStateStorage(context)
            // when
            val stateOut = stateStorage.getTaskEngineState(stateIn.taskId)
            // then
            verify(exactly = 1) { context.getState("engine.state.${stateIn.taskId.id}") }
            confirmVerified(context)
            stateOut shouldBe stateIn
        }
    }

    context("AvroKeyValueStateStorage.updateEngineState") {
        should("record state") {
            // mocking
            val context = mockk<Storage>()
            val stateIn = TestFactory.random(TaskEngineState::class)
            every { context.putState(any(), any()) } returns Unit
            // given
            val stateStorage = AvroKeyValueStateStorage(context)
            // when
            stateStorage.updateTaskEngineState(stateIn.taskId, stateIn, null)
            // then
            verify(exactly = 1) {
                context.putState(
                    "engine.state.${stateIn.taskId.id}",
                    byteBufferRepresentation(stateIn)
                )
            }
            confirmVerified(context)
        }
    }

    context("AvroKeyValueStateStorage.deleteEngineState") {
        should("delete state") {
            // mocking
            val context = mockk<Storage>()
            val stateIn = TestFactory.random(TaskEngineState::class)
            every { context.deleteState(any()) } returns Unit
            // given
            val stageStorage = AvroKeyValueStateStorage(context)
            // when
            stageStorage.deleteTaskEngineState(stateIn.taskId)
            // then
            verify(exactly = 1) { context.deleteState("engine.state.${stateIn.taskId.id}") }
            confirmVerified(context)
        }
    }

    context("AvroKeyValueStateStorage.getMonitoringPerNameState") {

        should("return null when state does not exist") {
            // given
            val taskName = TaskName(TestFactory.random(String::class))
            val context = mockk<Storage>()
            every { context.getState(any()) } returns null
            // when
            val stateStorage = AvroKeyValueStateStorage(context)
            val state = stateStorage.getMonitoringPerNameState(taskName)
            // then
            verify(exactly = 1) { context.getState("monitoringPerName.state.${taskName.name}") }
            confirmVerified(context)
            state shouldBe null
        }

        should("return state when state exists") {
            // given
            val stateIn = TestFactory.random(MonitoringPerNameState::class)
            val context = mockk<Storage>()
            every { context.getState(any()) } returns byteBufferRepresentation(stateIn)
            // when
            val stateStorage = AvroKeyValueStateStorage(context)
            val stateOut = stateStorage.getMonitoringPerNameState(stateIn.taskName)
            // then
            verify(exactly = 1) { context.getState("monitoringPerName.state.${stateIn.taskName.name}") }
            confirmVerified(context)
            stateOut shouldBe stateIn
        }
    }

    context("AvroKeyValueStateStorage.updateMonitoringPerNameState") {

        should("initializes all counters when old state is null and save state") {
            val context = mockk<Storage>()
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
            every { context.incrementCounter(any(), any()) } just runs
            every { context.getState(any()) } returns mockk()
            every { context.getCounter(any()) } returnsMany listOf(14L, 2L, 1L, 30L, 100L)
            every { context.putState(any(), any()) } just runs

            mockkObject(AvroConverter)

            val stateStorage = AvroKeyValueStateStorage(context)
            stateStorage.updateMonitoringPerNameState(newState.taskName, newState, null)

            verifyAll {
                context.incrementCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_OK), newState.runningOkCount)
                context.incrementCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_WARNING), newState.runningWarningCount)
                context.incrementCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_ERROR), newState.runningErrorCount)
                context.incrementCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_COMPLETED), newState.terminatedCompletedCount)
                context.incrementCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_CANCELED), newState.terminatedCanceledCount)
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_OK))
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_WARNING))
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_ERROR))
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_CANCELED))
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_COMPLETED))
                context.putState(stateStorage.getMonitoringPerNameStateKey(newState.taskName), ofType())
            }

            unmockkAll()
        }

        should("increment and decrement counters accordingly") {
            val context = mockk<Storage>()
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
            every { context.incrementCounter(any(), any()) } just runs
            every { context.getState(any()) } returns mockk()
            every { context.getCounter(any()) } returnsMany listOf(14L, 2L, 1L, 30L, 100L)
            every { context.putState(any(), any()) } just runs

            mockkObject(AvroConverter)

            val stateStorage = AvroKeyValueStateStorage(context)
            stateStorage.updateMonitoringPerNameState(newState.taskName, newState, oldState)

            verifyAll {
                context.incrementCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_ERROR), newState.runningErrorCount - oldState.runningErrorCount)
                context.incrementCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_CANCELED), newState.terminatedCanceledCount - oldState.terminatedCanceledCount)
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_OK))
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_WARNING))
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_ERROR))
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_CANCELED))
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_COMPLETED))
                context.putState(stateStorage.getMonitoringPerNameStateKey(newState.taskName), ofType())
            }

            unmockkAll()
        }
    }

    context("AvroKeyValueStateStorage.deleteMonitoringPerNameState") {
        should("should delete state") {
            // given
            val stateIn = TestFactory.random(MonitoringPerNameState::class)
            val context = mockk<Storage>()
            every { context.deleteState(any()) } returns Unit
            // when
            val stateStorage = AvroKeyValueStateStorage(context)
            stateStorage.deleteMonitoringPerNameState(stateIn.taskName)
            // then
            verify(exactly = 1) { context.deleteState(stateStorage.getMonitoringPerNameStateKey(stateIn.taskName)) }
            confirmVerified(context)
        }
    }

    context("AvroKeyValueStateStorage.getMonitoringGlobalState") {
        should("return null when state does not exist") {
            // mocking
            val context = mockk<Storage>()
            every { context.getState(any()) } returns null
            // given
            val stateStorage = AvroKeyValueStateStorage(context)
            // when
            val state = stateStorage.getMonitoringGlobalState()
            // then
            verify(exactly = 1) { context.getState("monitoringGlobal.state") }
            confirmVerified(context)
            state shouldBe null
        }

        should("return state when state exists") {
            // mocking
            val context = mockk<Storage>()
            val stateIn = TestFactory.random(MonitoringGlobalState::class)
            every { context.getState(any()) } returns byteBufferRepresentation(stateIn)
            // given
            val stateStorage = AvroKeyValueStateStorage(context)
            // when
            val stateOut = stateStorage.getMonitoringGlobalState()
            // then
            verify(exactly = 1) { context.getState("monitoringGlobal.state") }
            confirmVerified(context)
            stateOut shouldBe stateIn
        }
    }

    context("AvroKeyValueStateStorage.updateMonitoringGlobalState") {
        should("record state") {
            // mocking
            val context = mockk<Storage>()
            val stateIn = TestFactory.random(MonitoringGlobalState::class)
            every { context.putState(any(), any()) } returns Unit
            // given
            val stateStorage = AvroKeyValueStateStorage(context)
            // when
            stateStorage.updateMonitoringGlobalState(stateIn, null)
            // then
            verify(exactly = 1) {
                context.putState(
                    "monitoringGlobal.state",
                    byteBufferRepresentation(stateIn)
                )
            }
            confirmVerified(context)
        }
    }

    context("AvroKeyValueStateStorage.deleteMonitoringGlobalState") {
        should("delete state") {
            // mocking
            val context = mockk<Storage>()
            every { context.deleteState(any()) } returns Unit
            // given
            val stageStorage = AvroKeyValueStateStorage(context)
            // when
            stageStorage.deleteMonitoringGlobalState()
            // then
            verify(exactly = 1) { context.deleteState("monitoringGlobal.state") }
            confirmVerified(context)
        }
    }
})

private fun byteBufferRepresentation(state: State) = AvroConverter.toStorage(state).let { AvroSerDe.serialize(it) }
