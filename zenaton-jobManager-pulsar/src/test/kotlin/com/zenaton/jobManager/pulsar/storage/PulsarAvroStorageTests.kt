package com.zenaton.jobManager.pulsar.storage

import com.zenaton.commons.avro.AvroSerDe
import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.data.JobStatus
import com.zenaton.jobManager.pulsar.utils.TestFactory
import com.zenaton.jobManager.states.AvroJobEngineState
import com.zenaton.jobManager.states.AvroMonitoringGlobalState
import com.zenaton.jobManager.states.AvroMonitoringPerNameState
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

class PulsarAvroStorageTests : ShouldSpec({
    context("PulsarAvroStorage.getEngineState") {
        should("return null when state does not exist") {
            val jobId = TestFactory.get(String::class)
            // mocking
            val context = mockk<Context>()
            every { context.getState(any()) } returns null
            // given
            val stateStorage = PulsarAvroStorage(context)
            // when
            val state = stateStorage.getJobEngineState(jobId)
            // then
            verify(exactly = 1) { context.getState("engine.state.$jobId") }
            confirmVerified(context)
            state shouldBe null
        }

        should("return state when state exists") {
            // mocking
            val context = mockk<Context>()
            val stateIn = TestFactory.get(AvroJobEngineState::class)
            every { context.getState(any()) } returns AvroSerDe.serialize(stateIn)
            // given
            val stateStorage = PulsarAvroStorage(context)
            // when
            val stateOut = stateStorage.getJobEngineState(stateIn.jobId)
            // then
            verify(exactly = 1) { context.getState("engine.state.${stateIn.jobId}") }
            confirmVerified(context)
            stateOut shouldBe stateIn
        }
    }

    context("PulsarAvroStorage.updateEngineState") {
        should("record state") {
            // mocking
            val context = mockk<Context>()
            val stateIn = TestFactory.get(AvroJobEngineState::class)
            every { context.putState(any(), any()) } returns Unit
            // given
            val stateStorage = PulsarAvroStorage(context)
            // when
            stateStorage.updateJobEngineState(stateIn.jobId, stateIn, null)
            // then
            verify(exactly = 1) {
                context.putState(
                    "engine.state.${stateIn.jobId}",
                    AvroSerDe.serialize(stateIn)
                )
            }
            confirmVerified(context)
        }
    }

    context("PulsarAvroStorage.deleteEngineState") {
        should("delete state") {
            // mocking
            val context = mockk<Context>()
            val stateIn = TestFactory.get(AvroJobEngineState::class)
            every { context.deleteState(any()) } returns Unit
            // given
            val stageStorage = PulsarAvroStorage(context)
            // when
            stageStorage.deleteJobEngineState(stateIn.jobId)
            // then
            verify(exactly = 1) { context.deleteState("engine.state.${stateIn.jobId}") }
            confirmVerified(context)
        }
    }

    context("PulsarAvroStorage.getMonitoringPerNameState") {

        should("return null when state does not exist") {
            // given
            val jobName = TestFactory.get(String::class)
            val context = mockk<Context>()
            every { context.getState(any()) } returns null
            // when
            val stateStorage = PulsarAvroStorage(context)
            val state = stateStorage.getMonitoringPerNameState(jobName)
            // then
            verify(exactly = 1) { context.getState("monitoringPerName.state.$jobName") }
            confirmVerified(context)
            state shouldBe null
        }

        should("return state when state exists") {
            // given
            val stateIn = TestFactory.get(AvroMonitoringPerNameState::class)
            val context = mockk<Context>()
            every { context.getState(any()) } returns AvroSerDe.serialize(stateIn)
            // when
            val stateStorage = PulsarAvroStorage(context)
            val stateOut = stateStorage.getMonitoringPerNameState(stateIn.jobName)
            // then
            verify(exactly = 1) { context.getState("monitoringPerName.state.${stateIn.jobName}") }
            confirmVerified(context)
            stateOut shouldBe stateIn
        }
    }

    context("PulsarAvroStorage.updateMonitoringPerNameState") {

        should("initializes all counters when old state is null and save state") {
            val context = mockk<Context>()
            val newState = TestFactory.get(
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

            val stateStorage = PulsarAvroStorage(context)
            stateStorage.updateMonitoringPerNameState(newState.jobName, newState, null)

            verifyAll {
                context.incrCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.RUNNING_OK), newState.runningOkCount)
                context.incrCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.RUNNING_WARNING), newState.runningWarningCount)
                context.incrCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.RUNNING_ERROR), newState.runningErrorCount)
                context.incrCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.TERMINATED_COMPLETED), newState.terminatedCompletedCount)
                context.incrCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.TERMINATED_CANCELED), newState.terminatedCanceledCount)
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.RUNNING_OK))
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.RUNNING_WARNING))
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.RUNNING_ERROR))
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.TERMINATED_CANCELED))
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.TERMINATED_COMPLETED))
                context.putState(stateStorage.getMonitoringPerNameStateKey(newState.jobName), ofType())
            }

            unmockkAll()
        }

        should("increment and decrement counters accordingly") {
            val context = mockk<Context>()
            val oldState = TestFactory.get(
                AvroMonitoringPerNameState::class,
                mapOf(
                    "runningOkCount" to 10L,
                    "runningWarningCount" to 17L,
                    "terminatedCompletedCount" to 22L
                )
            )
            val newState = TestFactory.get(
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

            val stateStorage = PulsarAvroStorage(context)
            stateStorage.updateMonitoringPerNameState(newState.jobName, newState, oldState)

            verifyAll {
                context.incrCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.RUNNING_ERROR), newState.runningErrorCount - oldState.runningErrorCount)
                context.incrCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.TERMINATED_CANCELED), newState.terminatedCanceledCount - oldState.terminatedCanceledCount)
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.RUNNING_OK))
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.RUNNING_WARNING))
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.RUNNING_ERROR))
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.TERMINATED_CANCELED))
                context.getCounter(stateStorage.getMonitoringPerNameCounterKey(newState.jobName, JobStatus.TERMINATED_COMPLETED))
                context.putState(stateStorage.getMonitoringPerNameStateKey(newState.jobName), ofType())
            }

            unmockkAll()
        }
    }

    context("PulsarAvroStorage.deleteMonitoringPerNameState") {
        should("should delete state") {
            // given
            val stateIn = TestFactory.get(AvroMonitoringPerNameState::class)
            val context = mockk<Context>()
            every { context.deleteState(any()) } returns Unit
            // when
            val stateStorage = PulsarAvroStorage(context)
            stateStorage.deleteMonitoringPerNameState(stateIn.jobName)
            // then
            verify(exactly = 1) { context.deleteState(stateStorage.getMonitoringPerNameStateKey(stateIn.jobName)) }
            confirmVerified(context)
        }
    }

    context("PulsarAvroStorage.getMonitoringGlobalState") {
        should("return null when state does not exist") {
            // mocking
            val context = mockk<Context>()
            every { context.getState(any()) } returns null
            // given
            val stateStorage = PulsarAvroStorage(context)
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
            val stateIn = TestFactory.get(AvroMonitoringGlobalState::class)
            every { context.getState(any()) } returns AvroSerDe.serialize(stateIn)
            // given
            val stateStorage = PulsarAvroStorage(context)
            // when
            val stateOut = stateStorage.getMonitoringGlobalState()
            // then
            verify(exactly = 1) { context.getState("monitoringGlobal.state") }
            confirmVerified(context)
            stateOut shouldBe stateIn
        }
    }

    context("PulsarAvroStorage.updateMonitoringGlobalState") {
        should("record state") {
            // mocking
            val context = mockk<Context>()
            val stateIn = TestFactory.get(AvroMonitoringGlobalState::class)
            every { context.putState(any(), any()) } returns Unit
            // given
            val stateStorage = PulsarAvroStorage(context)
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

    context("PulsarAvroStorage.deleteMonitoringGlobalState") {
        should("delete state") {
            // mocking
            val context = mockk<Context>()
            every { context.deleteState(any()) } returns Unit
            // given
            val stageStorage = PulsarAvroStorage(context)
            // when
            stageStorage.deleteMonitoringGlobalState()
            // then
            verify(exactly = 1) { context.deleteState("monitoringGlobal.state") }
            confirmVerified(context)
        }
    }
})
