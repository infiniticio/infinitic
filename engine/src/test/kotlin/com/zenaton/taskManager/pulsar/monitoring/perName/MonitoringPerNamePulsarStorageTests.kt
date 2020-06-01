package com.zenaton.taskManager.pulsar.monitoring.perName

import com.zenaton.commons.utils.TestFactory
import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskManager.data.TaskName
import com.zenaton.taskManager.data.TaskStatus
import com.zenaton.taskManager.monitoring.perName.MonitoringPerNameState
import com.zenaton.taskManager.pulsar.avro.AvroConverter
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

class MonitoringPerNamePulsarStorageTests : ShouldSpec({
    context("PulsarTaskMetricsStateStorage.getState") {

        should("should return null if no state ") {
            // given
            val taskName = TestFactory.get(TaskName::class)
            val context = mockk<Context>()
            every { context.getState(any()) } returns null
            // when
            val stateStorage = MonitoringPerNamePulsarStorage(context)
            val state = stateStorage.getState(taskName)
            // then
            verify(exactly = 1) { context.getState(stateStorage.getStateKey(taskName)) }
            confirmVerified(context)
            state shouldBe null
        }

        should("should return deserialize state") {
            // given
            val stateIn = TestFactory.get(MonitoringPerNameState::class)
            val context = mockk<Context>()
            every { context.getState(any()) } returns AvroSerDe.serialize(AvroConverter.toAvro(stateIn))
            // when
            val stateStorage = MonitoringPerNamePulsarStorage(context)
            val stateOut = stateStorage.getState(stateIn.taskName)
            // then
            verify(exactly = 1) { context.getState(stateStorage.getStateKey(stateIn.taskName)) }
            confirmVerified(context)
            stateOut shouldBe stateIn
        }
    }

    context("PulsarTaskMetricsStateStorage.deleteState") {

        should("should delete state") {
            // given
            val stateIn = TestFactory.get(MonitoringPerNameState::class)
            val context = mockk<Context>()
            every { context.deleteState(any()) } returns Unit
            // when
            val stateStorage = MonitoringPerNamePulsarStorage(context)
            stateStorage.deleteState(stateIn.taskName)
            // then
            verify(exactly = 1) { context.deleteState(stateStorage.getStateKey(stateIn.taskName)) }
            confirmVerified(context)
        }
    }

    context("PulsarTaskMetricsStateStorage.updateState") {

        should("only increment new counter when old state is null and save state") {
            val context = mockk<Context>()
            val newState = TestFactory.get(
                MonitoringPerNameState::class, mapOf(
                "runningErrorCount" to 0L,
                "terminatedCanceledCount" to 0L
            ))
            every { context.incrCounter(any(), any()) } just runs
            every { context.getState(any()) } returns mockk()
            every { context.getCounter(any()) } returnsMany listOf(14L, 2L, 1L, 30L, 100L)
            every { context.putState(any(), any()) } just runs

            mockkObject(AvroConverter)

            val stateStorage = MonitoringPerNamePulsarStorage(context)
            stateStorage.updateState(newState.taskName, newState, null)

            verifyAll {
                context.incrCounter(stateStorage.getCounterKey(newState.taskName, TaskStatus.RUNNING_OK), newState.runningOkCount)
                context.incrCounter(stateStorage.getCounterKey(newState.taskName, TaskStatus.RUNNING_WARNING), newState.runningWarningCount)
                context.incrCounter(stateStorage.getCounterKey(newState.taskName, TaskStatus.TERMINATED_COMPLETED), newState.terminatedCompletedCount)
                context.getCounter(stateStorage.getCounterKey(newState.taskName, TaskStatus.RUNNING_OK))
                context.getCounter(stateStorage.getCounterKey(newState.taskName, TaskStatus.RUNNING_WARNING))
                context.getCounter(stateStorage.getCounterKey(newState.taskName, TaskStatus.RUNNING_ERROR))
                context.getCounter(stateStorage.getCounterKey(newState.taskName, TaskStatus.TERMINATED_CANCELED))
                context.getCounter(stateStorage.getCounterKey(newState.taskName, TaskStatus.TERMINATED_COMPLETED))
                AvroConverter.toAvro(MonitoringPerNameState(newState.taskName, 14L, 2L, 1L, 30L, 100L))
                context.putState(stateStorage.getStateKey(newState.taskName), ofType())
            }

            unmockkAll()
        }

        should("increment and decremen counters accordingly") {
            val context = mockk<Context>()
            val oldState = TestFactory.get(
                MonitoringPerNameState::class, mapOf(
                "runningOkCount" to 10L,
                "runningWarningCount" to 17L,
                "terminatedCompletedCount" to 22L
            ))
            val newState = TestFactory.get(
                MonitoringPerNameState::class, mapOf(
                "runningOkCount" to 10L,
                "runningWarningCount" to 17L,
                "terminatedCompletedCount" to 22L
            ))
            every { context.incrCounter(any(), any()) } just runs
            every { context.getState(any()) } returns mockk()
            every { context.getCounter(any()) } returnsMany listOf(14L, 2L, 1L, 30L, 100L)
            every { context.putState(any(), any()) } just runs

            mockkObject(AvroConverter)

            val stateStorage = MonitoringPerNamePulsarStorage(context)
            stateStorage.updateState(newState.taskName, newState, oldState)

            verifyAll {
                context.incrCounter(stateStorage.getCounterKey(newState.taskName, TaskStatus.RUNNING_ERROR), newState.runningErrorCount - oldState.runningErrorCount)
                context.incrCounter(stateStorage.getCounterKey(newState.taskName, TaskStatus.TERMINATED_CANCELED), newState.terminatedCanceledCount - oldState.terminatedCanceledCount)
                context.getCounter(stateStorage.getCounterKey(newState.taskName, TaskStatus.RUNNING_OK))
                context.getCounter(stateStorage.getCounterKey(newState.taskName, TaskStatus.RUNNING_WARNING))
                context.getCounter(stateStorage.getCounterKey(newState.taskName, TaskStatus.RUNNING_ERROR))
                context.getCounter(stateStorage.getCounterKey(newState.taskName, TaskStatus.TERMINATED_CANCELED))
                context.getCounter(stateStorage.getCounterKey(newState.taskName, TaskStatus.TERMINATED_COMPLETED))
                AvroConverter.toAvro(MonitoringPerNameState(newState.taskName, 14L, 2L, 1L, 30L, 100L))
                context.putState(stateStorage.getStateKey(newState.taskName), ofType())
            }

            unmockkAll()
        }
    }

    context("PulsarTaskMetricsStateStorage.updateTaskStatusCountersByName") {

        should("dispatch a message TaskMetricCreated for a new task name") {
//            val context = mockk<Context>()
//            every { context.incrCounter(any(), any()) } just runs
//            every { context.getState(any()) } returns null
//            every { context.getCounter(any()) } returnsMany listOf(1L, 0L, 0L, 0L, 0L)
//            every { context.putState(any(), any()) } just runs
//
//            val dispatcher = mockk<PulsarTaskDispatcher>()
//            every { dispatcher.dispatch(ofType<TaskMetricMessage>()) } just runs
//
//            mockkObject(TaskAvroConverter)
//
//            val stateStorage =
//                PulsarTaskMetricsStateStorage(context)
//            stateStorage.taskDispatcher = dispatcher
//            stateStorage.updateTaskStatusCountersByName(TaskName("SomeTask"), null, TaskStatus.RUNNING_OK)
//
//            verifyAll {
//                context.incrCounter("metrics.rt.counter.task.sometask.running_ok", 1L)
//                context.getState("metrics.task.sometask.counters")
//                context.getCounter("metrics.rt.counter.task.sometask.running_ok")
//                context.getCounter("metrics.rt.counter.task.sometask.running_warning")
//                context.getCounter("metrics.rt.counter.task.sometask.running_error")
//                context.getCounter("metrics.rt.counter.task.sometask.terminated_completed")
//                context.getCounter("metrics.rt.counter.task.sometask.terminated_canceled")
//                TaskAvroConverter.toAvro(TaskMetricsState(TaskName("SomeTask"), 1L, 0L, 0L, 0L, 0L))
//                context.putState("metrics.task.sometask.counters", ofType())
//                dispatcher.dispatch(ofType<TaskMetricCreated>())
//            }
//
//            unmockkAll()
        }
    }
})
