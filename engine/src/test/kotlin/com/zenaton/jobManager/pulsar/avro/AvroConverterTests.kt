package com.zenaton.jobManager.pulsar.avro

import com.zenaton.commons.utils.TestFactory
import com.zenaton.jobManager.engine.EngineState
import com.zenaton.jobManager.messages.AvroCancelJob
import com.zenaton.jobManager.messages.AvroDispatchJob
import com.zenaton.jobManager.messages.AvroJobAttemptCompleted
import com.zenaton.jobManager.messages.AvroJobAttemptDispatched
import com.zenaton.jobManager.messages.AvroJobAttemptFailed
import com.zenaton.jobManager.messages.AvroJobAttemptStarted
import com.zenaton.jobManager.messages.AvroJobCanceled
import com.zenaton.jobManager.messages.AvroJobCompleted
import com.zenaton.jobManager.messages.AvroJobCreated
import com.zenaton.jobManager.messages.AvroJobDispatched
import com.zenaton.jobManager.messages.AvroJobStatusUpdated
import com.zenaton.jobManager.messages.AvroRetryJob
import com.zenaton.jobManager.messages.AvroRetryJobAttempt
import com.zenaton.jobManager.messages.CancelJob
import com.zenaton.jobManager.messages.DispatchJob
import com.zenaton.jobManager.messages.JobAttemptCompleted
import com.zenaton.jobManager.messages.JobAttemptDispatched
import com.zenaton.jobManager.messages.JobAttemptFailed
import com.zenaton.jobManager.messages.JobAttemptStarted
import com.zenaton.jobManager.messages.JobCanceled
import com.zenaton.jobManager.messages.JobCompleted
import com.zenaton.jobManager.messages.JobCreated
import com.zenaton.jobManager.messages.JobDispatched
import com.zenaton.jobManager.messages.JobStatusUpdated
import com.zenaton.jobManager.messages.RetryJob
import com.zenaton.jobManager.messages.RetryJobAttempt
import com.zenaton.jobManager.messages.RunJob
import com.zenaton.jobManager.messages.interfaces.EngineMessage
import com.zenaton.jobManager.messages.interfaces.MonitoringGlobalMessage
import com.zenaton.jobManager.messages.interfaces.MonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.interfaces.MonitoringPerNameMessage
import com.zenaton.jobManager.messages.interfaces.WorkerMessage
import com.zenaton.jobManager.monitoring.perName.MonitoringPerNameState
import com.zenaton.jobManager.workers.AvroRunJob
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import org.apache.avro.specific.SpecificRecordBase

class AvroConverterTests : StringSpec({

    include(workerMessageShouldBeAvroReversible(RunJob::class, AvroRunJob::class))

    include(monitoringPerInstanceMessageShouldBeAvroReversible(JobAttemptDispatched::class, AvroJobAttemptDispatched::class))
    include(monitoringPerInstanceMessageShouldBeAvroReversible(JobCanceled::class, AvroJobCanceled::class))
    include(monitoringPerInstanceMessageShouldBeAvroReversible(JobCompleted::class, AvroJobCompleted::class))
    include(monitoringPerInstanceMessageShouldBeAvroReversible(JobDispatched::class, AvroJobDispatched::class))

    include(monitoringPerNameMessageShouldBeAvroReversible(JobStatusUpdated::class, AvroJobStatusUpdated::class))

    include(monitoringGlobalMessageShouldBeAvroReversible(JobCreated::class, AvroJobCreated::class))

    include(engineMessageShouldBeAvroReversible(CancelJob::class, AvroCancelJob::class))
    include(engineMessageShouldBeAvroReversible(DispatchJob::class, AvroDispatchJob::class))
    include(engineMessageShouldBeAvroReversible(RetryJob::class, AvroRetryJob::class))
    include(engineMessageShouldBeAvroReversible(RetryJobAttempt::class, AvroRetryJobAttempt::class))
    include(engineMessageShouldBeAvroReversible(JobAttemptCompleted::class, AvroJobAttemptCompleted::class))
    include(engineMessageShouldBeAvroReversible(JobAttemptFailed::class, AvroJobAttemptFailed::class))
    include(engineMessageShouldBeAvroReversible(JobAttemptStarted::class, AvroJobAttemptStarted::class))

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

inline fun <reified T : MonitoringGlobalMessage, P : SpecificRecordBase> monitoringGlobalMessageShouldBeAvroReversible(from: KClass<T>, to: KClass<P>) = stringSpec {
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

inline fun <reified T : MonitoringPerNameMessage, P : SpecificRecordBase> monitoringPerNameMessageShouldBeAvroReversible(from: KClass<T>, to: KClass<P>) = stringSpec {
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

inline fun <reified T : MonitoringPerInstanceMessage, P : SpecificRecordBase> monitoringPerInstanceMessageShouldBeAvroReversible(from: KClass<T>, to: KClass<P>) = stringSpec {
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
