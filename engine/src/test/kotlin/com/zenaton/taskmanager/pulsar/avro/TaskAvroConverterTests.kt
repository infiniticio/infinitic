package com.zenaton.taskmanager.pulsar.avro

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskmanager.data.TaskState
import com.zenaton.taskmanager.messages.engine.AvroCancelTask
import com.zenaton.taskmanager.messages.engine.AvroDispatchTask
import com.zenaton.taskmanager.messages.engine.AvroRetryTask
import com.zenaton.taskmanager.messages.engine.AvroRetryTaskAttempt
import com.zenaton.taskmanager.messages.engine.AvroTaskAttemptCompleted
import com.zenaton.taskmanager.messages.engine.AvroTaskAttemptDispatched
import com.zenaton.taskmanager.messages.engine.AvroTaskAttemptFailed
import com.zenaton.taskmanager.messages.engine.AvroTaskAttemptStarted
import com.zenaton.taskmanager.messages.engine.AvroTaskCanceled
import com.zenaton.taskmanager.messages.engine.AvroTaskCompleted
import com.zenaton.taskmanager.messages.engine.AvroTaskDispatched
import com.zenaton.taskmanager.messages.engine.CancelTask
import com.zenaton.taskmanager.messages.engine.DispatchTask
import com.zenaton.taskmanager.messages.engine.RetryTask
import com.zenaton.taskmanager.messages.engine.RetryTaskAttempt
import com.zenaton.taskmanager.messages.engine.TaskAttemptCompleted
import com.zenaton.taskmanager.messages.engine.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.engine.TaskAttemptFailed
import com.zenaton.taskmanager.messages.engine.TaskAttemptStarted
import com.zenaton.taskmanager.messages.engine.TaskCanceled
import com.zenaton.taskmanager.messages.engine.TaskCompleted
import com.zenaton.taskmanager.messages.engine.TaskDispatched
import com.zenaton.taskmanager.messages.engine.TaskEngineMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import org.apache.avro.specific.SpecificRecordBase

inline fun <reified T : TaskEngineMessage, P : SpecificRecordBase> shouldBeAvroReversible(from: KClass<T>, to: KClass<P>) = stringSpec {
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

class TaskAvroConverterTests : StringSpec({

    include(shouldBeAvroReversible(CancelTask::class, AvroCancelTask::class))
    include(shouldBeAvroReversible(DispatchTask::class, AvroDispatchTask::class))
    include(shouldBeAvroReversible(RetryTask::class, AvroRetryTask::class))
    include(shouldBeAvroReversible(RetryTaskAttempt::class, AvroRetryTaskAttempt::class))
    include(shouldBeAvroReversible(TaskAttemptCompleted::class, AvroTaskAttemptCompleted::class))
    include(shouldBeAvroReversible(TaskAttemptDispatched::class, AvroTaskAttemptDispatched::class))
    include(shouldBeAvroReversible(TaskAttemptFailed::class, AvroTaskAttemptFailed::class))
    include(shouldBeAvroReversible(TaskAttemptStarted::class, AvroTaskAttemptStarted::class))
    include(shouldBeAvroReversible(TaskCanceled::class, AvroTaskCanceled::class))
    include(shouldBeAvroReversible(TaskCompleted::class, AvroTaskCompleted::class))
    include(shouldBeAvroReversible(TaskDispatched::class, AvroTaskDispatched::class))

    "task state should be avroReversible" {
        // given
        val state = TestFactory.get(TaskState::class)
        // when
        val avroState = TaskAvroConverter.toAvro(state)
        val state2 = TaskAvroConverter.fromAvro(avroState)
        val avroState2 = TaskAvroConverter.toAvro(state2)
        // then
        state2 shouldBe state
        avroState2 shouldBe avroState
    }
})
