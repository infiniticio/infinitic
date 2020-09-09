package io.infinitic.taskManager.storage.pulsar

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.data.TaskStatus
import io.infinitic.taskManager.states.AvroTaskEngineState
import io.infinitic.taskManager.states.AvroMonitoringGlobalState
import io.infinitic.taskManager.states.AvroMonitoringPerNameState
import io.infinitic.taskManager.storage.pulsar.utils.TestFactory
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
import org.apache.pulsar.functions.api.Context

class PulsarStorageTests : ShouldSpec({
    context("PulsarStorage.getEngineState") {
        should("return null when state does not exist") {
            val taskId = TestFactory.random(String::class)
            // mocking
            val context = mockk<Context>()
            every { context.getState(any()) } returns null
            // given
            val stateStorage = PulsarStorage(context)
            // when
            val state = stateStorage.getTaskEngineState(taskId)
            // then
            verify(exactly = 1) { context.getState("engine.state.$taskId") }
            confirmVerified(context)
            state shouldBe null
        }

        should("return state when state exists") {
            // mocking
            val context = mockk<Context>()
            val stateIn = TestFactory.random(AvroTaskEngineState::class)
            every { context.getState(any()) } returns AvroSerDe.serialize(stateIn)
            // given
            val stateStorage = PulsarStorage(context)
            // when
            val stateOut = stateStorage.getTaskEngineState(stateIn.taskId)
            // then
            verify(exactly = 1) { context.getState("engine.state.${stateIn.taskId}") }
            confirmVerified(context)
            stateOut shouldBe stateIn
        }
    }

    context("PulsarStorage.updateEngineState") {
        should("record state") {
            // mocking
            val context = mockk<Context>()
            val stateIn = TestFactory.random(AvroTaskEngineState::class)
            every { context.putState(any(), any()) } returns Unit
            // given
            val stateStorage = PulsarStorage(context)
            // when
            stateStorage.updateTaskEngineState(stateIn.taskId, stateIn, null)
            // then
            verify(exactly = 1) {
                context.putState(
                    "engine.state.${stateIn.taskId}",
                    AvroSerDe.serialize(stateIn)
                )
            }
            confirmVerified(context)
        }
    }

    context("PulsarStorage.deleteEngineState") {
        should("delete state") {
            // mocking
            val context = mockk<Context>()
            val stateIn = TestFactory.random(AvroTaskEngineState::class)
            every { context.deleteState(any()) } returns Unit
            // given
            val stageStorage = PulsarStorage(context)
            // when
            stageStorage.deleteTaskEngineState(stateIn.taskId)
            // then
            verify(exactly = 1) { context.deleteState("engine.state.${stateIn.taskId}") }
            confirmVerified(context)
        }
    }

    context("PulsarStorage.getMonitoringPerNameState") {

        should("return null when state does not exist") {
            // given
            val taskName = TestFactory.random(String::class)
            val context = mockk<Context>()
            every { context.getState(any()) } returns null
            // when
            val stateStorage = PulsarStorage(context)
            val state = stateStorage.getMonitoringPerNameState(taskName)
            // then
            verify(exactly = 1) { context.getState("monitoringPerName.state.$taskName") }
            confirmVerified(context)
            state shouldBe null
        }

        should("return state when state exists") {
            // given
            val stateIn = TestFactory.random(AvroMonitoringPerNameState::class)
            val context = mockk<Context>()
            every { context.getState(any()) } returns AvroSerDe.serialize(stateIn)
            // when
            val stateStorage = PulsarStorage(context)
            val stateOut = stateStorage.getMonitoringPerNameState(stateIn.taskName)
            // then
            verify(exactly = 1) { context.getState("monitoringPerName.state.${stateIn.taskName}") }
            confirmVerified(context)
            stateOut shouldBe stateIn
        }
    }

    context("PulsarStorage.updateMonitoringPerNameState") {

        should("initializes all counters when old state is null and save state") {
            val context = mockk<Context>()
            val newState = TestFactory.random(
                AvroMonitoringPerNameState::class,
                mapOf(
                    "runningOkCount" to 1L,
                    "runningWarningCount" to 0L,
                    "runningErrorCount" to 0L,
                    "terminatedCompletedCount" to 0L,
                    "terminatedCanceledCount" to 0L
                )
            )
            every { context.incrCounter(any(), any()) } just runs
            every { context.getState(any()) } returns mockk()
            every { context.getCounter(any()) } returnsMany listOf(14L, 2L, 1L, 30L, 100L)
            every { context.putState(any(), any()) } just runs

            mockkObject(AvroConverter)

            val stateStorage = PulsarStorage(context)
            stateStorage.updateMonitoringPerNameState(newState.taskName, newState, null)

            verifyAll {
                context.incrCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_OK), newState.runningOkCount)
                context.incrCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_WARNING), newState.runningWarningCount)
                context.incrCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_ERROR), newState.runningErrorCount)
                context.incrCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_COMPLETED), newState.terminatedCompletedCount)
                context.incrCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_CANCELED), newState.terminatedCanceledCount)
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
            val context = mockk<Context>()
            val oldState = TestFactory.random(
                AvroMonitoringPerNameState::class,
                mapOf(
                    "runningOkCount" to 10L,
                    "runningWarningCount" to 17L,
                    "terminatedCompletedCount" to 22L
                )
            )
            val newState = TestFactory.random(
                AvroMonitoringPerNameState::class,
                mapOf(
                    "runningOkCount" to 10L,
                    "runningWarningCount" to 17L,
                    "terminatedCompletedCount" to 22L
                )
            )
            every { context.incrCounter(any(), any()) } just runs
            every { context.getState(any()) } returns mockk()
            every { context.getCounter(any()) } returnsMany listOf(14L, 2L, 1L, 30L, 100L)
            every { context.putState(any(), any()) } just runs

            mockkObject(AvroConverter)

            val stateStorage = PulsarStorage(context)
            stateStorage.updateMonitoringPerNameState(newState.taskName, newState, oldState)

            verifyAll {
                context.incrCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.RUNNING_ERROR), newState.runningErrorCount - oldState.runningErrorCount)
                context.incrCounter(stateStorage.getMonitoringPerNameCounterKey(newState.taskName, TaskStatus.TERMINATED_CANCELED), newState.terminatedCanceledCount - oldState.terminatedCanceledCount)
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

    context("PulsarStorage.deleteMonitoringPerNameState") {
        should("should delete state") {
            // given
            val stateIn = TestFactory.random(AvroMonitoringPerNameState::class)
            val context = mockk<Context>()
            every { context.deleteState(any()) } returns Unit
            // when
            val stateStorage = PulsarStorage(context)
            stateStorage.deleteMonitoringPerNameState(stateIn.taskName)
            // then
            verify(exactly = 1) { context.deleteState(stateStorage.getMonitoringPerNameStateKey(stateIn.taskName)) }
            confirmVerified(context)
        }
    }

    context("PulsarStorage.getMonitoringGlobalState") {
        should("return null when state does not exist") {
            // mocking
            val context = mockk<Context>()
            every { context.getState(any()) } returns null
            // given
            val stateStorage = PulsarStorage(context)
            // when
            val state = stateStorage.getMonitoringGlobalState()
            // then
            verify(exactly = 1) { context.getState("monitoringGlobal.state") }
            confirmVerified(context)
            state shouldBe null
        }

        should("return state when state exists") {
            // mocking
            val context = mockk<Context>()
            val stateIn = TestFactory.random(AvroMonitoringGlobalState::class)
            every { context.getState(any()) } returns AvroSerDe.serialize(stateIn)
            // given
            val stateStorage = PulsarStorage(context)
            // when
            val stateOut = stateStorage.getMonitoringGlobalState()
            // then
            verify(exactly = 1) { context.getState("monitoringGlobal.state") }
            confirmVerified(context)
            stateOut shouldBe stateIn
        }
    }

    context("PulsarStorage.updateMonitoringGlobalState") {
        should("record state") {
            // mocking
            val context = mockk<Context>()
            val stateIn = TestFactory.random(AvroMonitoringGlobalState::class)
            every { context.putState(any(), any()) } returns Unit
            // given
            val stateStorage = PulsarStorage(context)
            // when
            stateStorage.updateMonitoringGlobalState(stateIn, null)
            // then
            verify(exactly = 1) {
                context.putState(
                    "monitoringGlobal.state",
                    AvroSerDe.serialize(stateIn)
                )
            }
            confirmVerified(context)
        }
    }

    context("PulsarStorage.deleteMonitoringGlobalState") {
        should("delete state") {
            // mocking
            val context = mockk<Context>()
            every { context.deleteState(any()) } returns Unit
            // given
            val stageStorage = PulsarStorage(context)
            // when
            stageStorage.deleteMonitoringGlobalState()
            // then
            verify(exactly = 1) { context.deleteState("monitoringGlobal.state") }
            confirmVerified(context)
        }
    }
})
