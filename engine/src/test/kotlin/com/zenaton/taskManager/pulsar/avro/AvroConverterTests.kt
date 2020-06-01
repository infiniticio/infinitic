package com.zenaton.taskManager.pulsar.avro

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskManager.admin.messages.AvroTaskCreated
import com.zenaton.taskManager.monitoring.global.MonitoringGlobalMessage
import com.zenaton.taskManager.monitoring.global.TaskCreated
import com.zenaton.taskManager.engine.messages.AvroCancelTask
import com.zenaton.taskManager.engine.messages.AvroDispatchTask
import com.zenaton.taskManager.engine.messages.AvroRetryTask
import com.zenaton.taskManager.engine.messages.AvroRetryTaskAttempt
import com.zenaton.taskManager.engine.messages.AvroTaskAttemptCompleted
import com.zenaton.taskManager.engine.messages.AvroTaskAttemptDispatched
import com.zenaton.taskManager.engine.messages.AvroTaskAttemptFailed
import com.zenaton.taskManager.engine.messages.AvroTaskAttemptStarted
import com.zenaton.taskManager.engine.messages.AvroTaskCanceled
import com.zenaton.taskManager.engine.messages.AvroTaskCompleted
import com.zenaton.taskManager.engine.messages.AvroTaskDispatched
import com.zenaton.taskManager.engine.CancelTask
import com.zenaton.taskManager.engine.DispatchTask
import com.zenaton.taskManager.engine.RetryTask
import com.zenaton.taskManager.engine.RetryTaskAttempt
import com.zenaton.taskManager.engine.TaskAttemptCompleted
import com.zenaton.taskManager.engine.TaskAttemptDispatched
import com.zenaton.taskManager.engine.TaskAttemptFailed
import com.zenaton.taskManager.engine.TaskAttemptStarted
import com.zenaton.taskManager.engine.TaskCanceled
import com.zenaton.taskManager.engine.TaskCompleted
import com.zenaton.taskManager.engine.TaskDispatched
import com.zenaton.taskManager.engine.EngineMessage
import com.zenaton.taskManager.engine.EngineState
import com.zenaton.taskManager.metrics.messages.AvroTaskStatusUpdated
import com.zenaton.taskManager.monitoring.perName.MonitoringPerNameMessage
import com.zenaton.taskManager.monitoring.perName.TaskStatusUpdated
import com.zenaton.taskManager.monitoring.perName.MonitoringPerNameState
import com.zenaton.taskManager.workers.AvroRunTask
import com.zenaton.taskManager.workers.RunTask
import com.zenaton.taskManager.workers.WorkerMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import org.apache.avro.specific.SpecificRecordBase

class AvroConverterTests : StringSpec({

    include(workerMessageShouldBeAvroReversible(RunTask::class, AvroRunTask::class))

    include(metricMessageShouldBeAvroReversible(TaskStatusUpdated::class, AvroTaskStatusUpdated::class))

    include(adminMessageShouldBeAvroReversible(TaskCreated::class, AvroTaskCreated::class))

    include(engineMessageShouldBeAvroReversible(CancelTask::class, AvroCancelTask::class))
    include(engineMessageShouldBeAvroReversible(DispatchTask::class, AvroDispatchTask::class))
    include(engineMessageShouldBeAvroReversible(RetryTask::class, AvroRetryTask::class))
    include(engineMessageShouldBeAvroReversible(RetryTaskAttempt::class, AvroRetryTaskAttempt::class))
    include(engineMessageShouldBeAvroReversible(TaskAttemptCompleted::class, AvroTaskAttemptCompleted::class))
    include(engineMessageShouldBeAvroReversible(TaskAttemptDispatched::class, AvroTaskAttemptDispatched::class))
    include(engineMessageShouldBeAvroReversible(TaskAttemptFailed::class, AvroTaskAttemptFailed::class))
    include(engineMessageShouldBeAvroReversible(TaskAttemptStarted::class, AvroTaskAttemptStarted::class))
    include(engineMessageShouldBeAvroReversible(TaskCanceled::class, AvroTaskCanceled::class))
    include(engineMessageShouldBeAvroReversible(TaskCompleted::class, AvroTaskCompleted::class))
    include(engineMessageShouldBeAvroReversible(TaskDispatched::class, AvroTaskDispatched::class))

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
