package com.zenaton.taskmanager.pulsar.state

import com.zenaton.commons.utils.TestFactory
import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskmanager.metrics.state.TaskMetricsState
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifyAll
import org.apache.pulsar.functions.api.Context

class PulsarTaskMetricsStateStorageTests : ShouldSpec({
    context("PulsarTaskMetricsStateStorageTests.getState") {
        should("return null for non-existing state") {
            val context = mockk<Context>()
            every { context.getState(any()) } returns null

            PulsarTaskMetricsStateStorage(context).getState("key") shouldBe null
        }

        should("return deserialized state for existing state") {
            val context = mockk<Context>()
            val stateIn = TestFactory.get(TaskMetricsState::class)
            every { context.getState(any()) } returns AvroSerDe.serialize(TaskAvroConverter.toAvro(stateIn))

            PulsarTaskMetricsStateStorage(context).getState("key") shouldBe stateIn
        }
    }

    context("PulsarTaskMetricsStateStorageTests.putState") {
        should("record serialized state") {
            val context = mockk<Context>()
            val stateIn = TestFactory.get(TaskMetricsState::class)
            every { context.putState(any(), any()) } just runs

            val stateStorage = PulsarTaskMetricsStateStorage(context)

            val key = TestFactory.get(String::class)
            stateStorage.putState(key, stateIn)

            verifyAll { context.putState(key, AvroSerDe.serialize(TaskAvroConverter.toAvro(stateIn))) }
        }
    }

    context("PulsarTaskMetricsStateStorageTests.deleteState") {
        should("delete existing state") {
            val context = mockk<Context>()
            every { context.deleteState(any()) } just runs

            val stageStorage = PulsarTaskMetricsStateStorage(context)

            val key = TestFactory.get(String::class)
            stageStorage.deleteState(key)

            verifyAll { context.deleteState(key) }
        }
    }
})
