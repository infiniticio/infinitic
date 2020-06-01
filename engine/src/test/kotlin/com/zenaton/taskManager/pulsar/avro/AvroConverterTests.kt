package com.zenaton.taskManager.pulsar.avro

import com.zenaton.commons.utils.TestFactory
import com.zenaton.jobManager.admin.messages.AvroJobCreated
import com.zenaton.jobManager.engine.messages.AvroCancelJob
import com.zenaton.jobManager.engine.messages.AvroDispatchJob
import com.zenaton.jobManager.engine.messages.AvroJobAttemptCompleted
import com.zenaton.jobManager.engine.messages.AvroJobAttemptDispatched
import com.zenaton.jobManager.engine.messages.AvroJobAttemptFailed
import com.zenaton.jobManager.engine.messages.AvroJobAttemptStarted
import com.zenaton.jobManager.engine.messages.AvroJobCanceled
import com.zenaton.jobManager.engine.messages.AvroJobCompleted
import com.zenaton.jobManager.engine.messages.AvroJobDispatched
import com.zenaton.jobManager.engine.messages.AvroRetryJob
import com.zenaton.jobManager.engine.messages.AvroRetryJobAttempt
import com.zenaton.jobManager.metrics.messages.AvroJobStatusUpdated
import com.zenaton.jobManager.workers.AvroRunJob
import com.zenaton.taskManager.engine.CancelJob
import com.zenaton.taskManager.engine.DispatchJob
import com.zenaton.taskManager.engine.EngineMessage
import com.zenaton.taskManager.engine.EngineState
import com.zenaton.taskManager.engine.JobAttemptCompleted
import com.zenaton.taskManager.engine.JobAttemptDispatched
import com.zenaton.taskManager.engine.JobAttemptFailed
import com.zenaton.taskManager.engine.JobAttemptStarted
import com.zenaton.taskManager.engine.JobCanceled
import com.zenaton.taskManager.engine.JobCompleted
import com.zenaton.taskManager.engine.JobDispatched
import com.zenaton.taskManager.engine.RetryJob
import com.zenaton.taskManager.engine.RetryJobAttempt
import com.zenaton.taskManager.monitoring.global.JobCreated
import com.zenaton.taskManager.monitoring.global.MonitoringGlobalMessage
import com.zenaton.taskManager.monitoring.perName.JobStatusUpdated
import com.zenaton.taskManager.monitoring.perName.MonitoringPerNameMessage
import com.zenaton.taskManager.monitoring.perName.MonitoringPerNameState
import com.zenaton.taskManager.workers.RunJob
import com.zenaton.taskManager.workers.WorkerMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import org.apache.avro.specific.SpecificRecordBase

class AvroConverterTests : StringSpec({

    include(workerMessageShouldBeAvroReversible(RunJob::class, AvroRunJob::class))

    include(metricMessageShouldBeAvroReversible(JobStatusUpdated::class, AvroJobStatusUpdated::class))

    include(adminMessageShouldBeAvroReversible(JobCreated::class, AvroJobCreated::class))

    include(engineMessageShouldBeAvroReversible(CancelJob::class, AvroCancelJob::class))
    include(engineMessageShouldBeAvroReversible(DispatchJob::class, AvroDispatchJob::class))
    include(engineMessageShouldBeAvroReversible(RetryJob::class, AvroRetryJob::class))
    include(engineMessageShouldBeAvroReversible(RetryJobAttempt::class, AvroRetryJobAttempt::class))
    include(engineMessageShouldBeAvroReversible(JobAttemptCompleted::class, AvroJobAttemptCompleted::class))
    include(engineMessageShouldBeAvroReversible(JobAttemptDispatched::class, AvroJobAttemptDispatched::class))
    include(engineMessageShouldBeAvroReversible(JobAttemptFailed::class, AvroJobAttemptFailed::class))
    include(engineMessageShouldBeAvroReversible(JobAttemptStarted::class, AvroJobAttemptStarted::class))
    include(engineMessageShouldBeAvroReversible(JobCanceled::class, AvroJobCanceled::class))
    include(engineMessageShouldBeAvroReversible(JobCompleted::class, AvroJobCompleted::class))
    include(engineMessageShouldBeAvroReversible(JobDispatched::class, AvroJobDispatched::class))

    "task state should be avroReversible" {
        // given
        val state = TestFactory.get(EngineState::class)
        // when
        val avroState = AvroConverter.toAvro(state)
        val state2 = AvroConverter.fromAvro(avroState)
        val avroState2 = AvroConverter.toAvro(state2)
        // then
        state2 shouldBe state
        avroState2 shouldBe avroState
    }

    "task metrics state should be avroReversible" {
        // given
        val state = TestFactory.get(MonitoringPerNameState::class)
        // when
        val avroState = AvroConverter.toAvro(state)
        val state2 = AvroConverter.fromAvro(avroState)
        val avroState2 = AvroConverter.toAvro(state2)
        // then
        state2 shouldBe state
        avroState2 shouldBe avroState
    }
})

inline fun <reified T : EngineMessage, P : SpecificRecordBase> engineMessageShouldBeAvroReversible(from: KClass<T>, to: KClass<P>) = stringSpec {
    // given
    val msg = TestFactory.get(from)
    // when
    val avroMsg = AvroConverter.toAvro(msg)
    val msg2 = AvroConverter.fromAvro(avroMsg)
    val avroMsg2 = AvroConverter.toAvro(msg2)
    // then
    msg shouldBe msg2
    avroMsg shouldBe avroMsg2
}

inline fun <reified T : MonitoringGlobalMessage, P : SpecificRecordBase> adminMessageShouldBeAvroReversible(from: KClass<T>, to: KClass<P>) = stringSpec {
    // given
    val msg = TestFactory.get(from)
    // when
    val avroMsg = AvroConverter.toAvro(msg)
    val msg2 = AvroConverter.fromAvro(avroMsg)
    val avroMsg2 = AvroConverter.toAvro(msg2)
    // then
    msg shouldBe msg2
    avroMsg shouldBe avroMsg2
}

inline fun <reified T : MonitoringPerNameMessage, P : SpecificRecordBase> metricMessageShouldBeAvroReversible(from: KClass<T>, to: KClass<P>) = stringSpec {
    // given
    val msg = TestFactory.get(from)
    // when
    val avroMsg = AvroConverter.toAvro(msg)
    val msg2 = AvroConverter.fromAvro(avroMsg)
    val avroMsg2 = AvroConverter.toAvro(msg2)
    // then
    msg shouldBe msg2
    avroMsg shouldBe avroMsg2
}

inline fun <reified T : WorkerMessage, P : SpecificRecordBase> workerMessageShouldBeAvroReversible(from: KClass<T>, to: KClass<P>) = stringSpec {
    // given
    val msg = TestFactory.get(from)
    // when
    val avroMsg = AvroConverter.toAvro(msg)
    val msg2 = AvroConverter.fromAvro(avroMsg)
    val avroMsg2 = AvroConverter.toAvro(msg2)
    // then
    msg shouldBe msg2
    avroMsg shouldBe avroMsg2
}
