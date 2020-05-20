package com.zenaton.taskmanager.pulsar.avro

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskmanager.messages.TaskMessageInterface
import com.zenaton.taskmanager.messages.commands.AvroCancelTask
import com.zenaton.taskmanager.messages.commands.AvroDispatchTask
import com.zenaton.taskmanager.messages.commands.AvroRetryTask
import com.zenaton.taskmanager.messages.commands.AvroRetryTaskAttempt
import com.zenaton.taskmanager.messages.commands.CancelTask
import com.zenaton.taskmanager.messages.commands.DispatchTask
import com.zenaton.taskmanager.messages.commands.RetryTask
import com.zenaton.taskmanager.messages.commands.RetryTaskAttempt
import com.zenaton.taskmanager.messages.events.AvroTaskAttemptCompleted
import com.zenaton.taskmanager.messages.events.AvroTaskAttemptDispatched
import com.zenaton.taskmanager.messages.events.AvroTaskAttemptFailed
import com.zenaton.taskmanager.messages.events.AvroTaskAttemptStarted
import com.zenaton.taskmanager.messages.events.AvroTaskCanceled
import com.zenaton.taskmanager.messages.events.AvroTaskCompleted
import com.zenaton.taskmanager.messages.events.AvroTaskDispatched
import com.zenaton.taskmanager.messages.events.TaskAttemptCompleted
import com.zenaton.taskmanager.messages.events.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.events.TaskAttemptFailed
import com.zenaton.taskmanager.messages.events.TaskAttemptStarted
import com.zenaton.taskmanager.messages.events.TaskCanceled
import com.zenaton.taskmanager.messages.events.TaskCompleted
import com.zenaton.taskmanager.messages.events.TaskDispatched
import com.zenaton.taskmanager.state.TaskState
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import org.apache.avro.specific.SpecificRecordBase

inline fun <reified T : TaskMessageInterface, P : SpecificRecordBase> shouldBeAvroReversible(from: KClass<T>, to: KClass<P>) = stringSpec {
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
