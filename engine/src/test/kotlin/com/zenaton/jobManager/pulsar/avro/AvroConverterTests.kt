package com.zenaton.jobManager.pulsar.avro

import com.zenaton.commons.utils.TestFactory
import com.zenaton.jobManager.messages.monitoring.global.AvroJobCreated
import com.zenaton.jobManager.engine.CancelJob
import com.zenaton.jobManager.engine.DispatchJob
import com.zenaton.jobManager.engine.EngineMessage
import com.zenaton.jobManager.engine.EngineState
import com.zenaton.jobManager.engine.JobAttemptCompleted
import com.zenaton.jobManager.engine.JobAttemptFailed
import com.zenaton.jobManager.engine.JobAttemptStarted
import com.zenaton.jobManager.engine.RetryJob
import com.zenaton.jobManager.engine.RetryJobAttempt
import com.zenaton.jobManager.messages.engine.AvroCancelJob
import com.zenaton.jobManager.messages.engine.AvroDispatchJob
import com.zenaton.jobManager.messages.engine.AvroJobAttemptCompleted
import com.zenaton.jobManager.messages.engine.AvroJobAttemptFailed
import com.zenaton.jobManager.messages.engine.AvroJobAttemptStarted
import com.zenaton.jobManager.messages.engine.AvroRetryJob
import com.zenaton.jobManager.messages.engine.AvroRetryJobAttempt
import com.zenaton.jobManager.messages.monitoring.perInstance.AvroJobAttemptDispatched
import com.zenaton.jobManager.messages.monitoring.perInstance.AvroJobCanceled
import com.zenaton.jobManager.messages.monitoring.perInstance.AvroJobCompleted
import com.zenaton.jobManager.messages.monitoring.perInstance.AvroJobDispatched
import com.zenaton.jobManager.messages.monitoring.perName.AvroJobStatusUpdated
import com.zenaton.jobManager.monitoring.global.JobCreated
import com.zenaton.jobManager.monitoring.global.MonitoringGlobalMessage
import com.zenaton.jobManager.monitoring.perInstance.JobAttemptDispatched
import com.zenaton.jobManager.monitoring.perInstance.JobCanceled
import com.zenaton.jobManager.monitoring.perInstance.JobCompleted
import com.zenaton.jobManager.monitoring.perInstance.JobDispatched
import com.zenaton.jobManager.monitoring.perInstance.MonitoringPerInstanceMessage
import com.zenaton.jobManager.monitoring.perName.JobStatusUpdated
import com.zenaton.jobManager.monitoring.perName.MonitoringPerNameMessage
import com.zenaton.jobManager.monitoring.perName.MonitoringPerNameState
import com.zenaton.jobManager.workers.AvroRunJob
import com.zenaton.jobManager.workers.RunJob
import com.zenaton.jobManager.workers.WorkerMessage
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
