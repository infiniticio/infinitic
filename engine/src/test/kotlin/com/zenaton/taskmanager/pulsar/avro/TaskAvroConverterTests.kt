package com.zenaton.taskmanager.pulsar.avro

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskmanager.admin.messages.AvroTaskTypeCreated
import com.zenaton.taskmanager.admin.messages.TaskAdminMessage
import com.zenaton.taskmanager.admin.messages.TaskTypeCreated
import com.zenaton.taskmanager.engine.messages.AvroCancelTask
import com.zenaton.taskmanager.engine.messages.AvroDispatchTask
import com.zenaton.taskmanager.engine.messages.AvroRetryTask
import com.zenaton.taskmanager.engine.messages.AvroRetryTaskAttempt
import com.zenaton.taskmanager.engine.messages.AvroTaskAttemptCompleted
import com.zenaton.taskmanager.engine.messages.AvroTaskAttemptDispatched
import com.zenaton.taskmanager.engine.messages.AvroTaskAttemptFailed
import com.zenaton.taskmanager.engine.messages.AvroTaskAttemptStarted
import com.zenaton.taskmanager.engine.messages.AvroTaskCanceled
import com.zenaton.taskmanager.engine.messages.AvroTaskCompleted
import com.zenaton.taskmanager.engine.messages.AvroTaskDispatched
import com.zenaton.taskmanager.engine.messages.CancelTask
import com.zenaton.taskmanager.engine.messages.DispatchTask
import com.zenaton.taskmanager.engine.messages.RetryTask
import com.zenaton.taskmanager.engine.messages.RetryTaskAttempt
import com.zenaton.taskmanager.engine.messages.TaskAttemptCompleted
import com.zenaton.taskmanager.engine.messages.TaskAttemptDispatched
import com.zenaton.taskmanager.engine.messages.TaskAttemptFailed
import com.zenaton.taskmanager.engine.messages.TaskAttemptStarted
import com.zenaton.taskmanager.engine.messages.TaskCanceled
import com.zenaton.taskmanager.engine.messages.TaskCompleted
import com.zenaton.taskmanager.engine.messages.TaskDispatched
import com.zenaton.taskmanager.engine.messages.TaskEngineMessage
import com.zenaton.taskmanager.engine.state.TaskEngineState
import com.zenaton.taskmanager.metrics.messages.AvroTaskStatusUpdated
import com.zenaton.taskmanager.metrics.messages.TaskMetricMessage
import com.zenaton.taskmanager.metrics.messages.TaskStatusUpdated
import com.zenaton.taskmanager.metrics.state.TaskMetricsState
import com.zenaton.taskmanager.workers.AvroRunTask
import com.zenaton.taskmanager.workers.messages.RunTask
import com.zenaton.taskmanager.workers.messages.TaskWorkerMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import org.apache.avro.specific.SpecificRecordBase

class TaskAvroConverterTests : StringSpec({

    include(workerMessageShouldBeAvroReversible(RunTask::class, AvroRunTask::class))

    include(metricMessageShouldBeAvroReversible(TaskStatusUpdated::class, AvroTaskStatusUpdated::class))

    include(adminMessageShouldBeAvroReversible(TaskTypeCreated::class, AvroTaskTypeCreated::class))

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
        val state = TestFactory.get(TaskEngineState::class)
        // when
        val avroState = TaskAvroConverter.toAvro(state)
        val state2 = TaskAvroConverter.fromAvro(avroState)
        val avroState2 = TaskAvroConverter.toAvro(state2)
        // then
        state2 shouldBe state
        avroState2 shouldBe avroState
    }

    "task metrics state should be avroReversible" {
        // given
        val state = TestFactory.get(TaskMetricsState::class)
        // when
        val avroState = TaskAvroConverter.toAvro(state)
        val state2 = TaskAvroConverter.fromAvro(avroState)
        val avroState2 = TaskAvroConverter.toAvro(state2)
        // then
        state2 shouldBe state
        avroState2 shouldBe avroState
    }
})

inline fun <reified T : TaskEngineMessage, P : SpecificRecordBase> engineMessageShouldBeAvroReversible(from: KClass<T>, to: KClass<P>) = stringSpec {
    // given
    val msg = TestFactory.get(from)
    // when
    val avroMsg = TaskAvroConverter.toAvro(msg)
    val msg2 = TaskAvroConverter.fromAvro(avroMsg)
    val avroMsg2 = TaskAvroConverter.toAvro(msg2)
    // then
    msg shouldBe msg2
    avroMsg shouldBe avroMsg2
}

inline fun <reified T : TaskAdminMessage, P : SpecificRecordBase> adminMessageShouldBeAvroReversible(from: KClass<T>, to: KClass<P>) = stringSpec {
    // given
    val msg = TestFactory.get(from)
    // when
    val avroMsg = TaskAvroConverter.toAvro(msg)
    val msg2 = TaskAvroConverter.fromAvro(avroMsg)
    val avroMsg2 = TaskAvroConverter.toAvro(msg2)
    // then
    msg shouldBe msg2
    avroMsg shouldBe avroMsg2
}

inline fun <reified T : TaskMetricMessage, P : SpecificRecordBase> metricMessageShouldBeAvroReversible(from: KClass<T>, to: KClass<P>) = stringSpec {
    // given
    val msg = TestFactory.get(from)
    // when
    val avroMsg = TaskAvroConverter.toAvro(msg)
    val msg2 = TaskAvroConverter.fromAvro(avroMsg)
    val avroMsg2 = TaskAvroConverter.toAvro(msg2)
    // then
    msg shouldBe msg2
    avroMsg shouldBe avroMsg2
}

inline fun <reified T : TaskWorkerMessage, P : SpecificRecordBase> workerMessageShouldBeAvroReversible(from: KClass<T>, to: KClass<P>) = stringSpec {
    // given
    val msg = TestFactory.get(from)
    // when
    val avroMsg = TaskAvroConverter.toAvro(msg)
    val msg2 = TaskAvroConverter.fromAvro(avroMsg)
    val avroMsg2 = TaskAvroConverter.toAvro(msg2)
    // then
    msg shouldBe msg2
    avroMsg shouldBe avroMsg2
}
