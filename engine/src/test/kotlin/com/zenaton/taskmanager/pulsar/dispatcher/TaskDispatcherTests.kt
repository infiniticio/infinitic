package com.zenaton.taskmanager.pulsar.dispatcher

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskmanager.messages.engine.AvroTaskEngineMessage
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
import com.zenaton.taskmanager.messages.workers.AvroRunTask
import com.zenaton.taskmanager.messages.workers.RunTask
import com.zenaton.taskmanager.pulsar.Topic
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.reflect.KClass
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.TypedMessageBuilder
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

fun <T : TaskEngineMessage> dispatchShouldSendTaskMessageToTasksTopic(klass: KClass<T>) = stringSpec {
    // mocking
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroTaskEngineMessage>>()
    val slotSchema = slot<AvroSchema<AvroTaskEngineMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroTaskEngineMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.key(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // given
    val msg = TestFactory.get(klass)
    // when
    TaskDispatcher(context).dispatch(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.TASKS.get()
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroTaskEngineMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(TaskAvroConverter.toAvro(msg)) }
    verify(exactly = 1) { builder.key(msg.getStateId()) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}

class TaskDispatcherTests : StringSpec({
    "dispatch methods should synchronously send a RunTask message to task-specific topic" {
        // mocking
        val context = mockk<Context>()
        val builder = mockk<TypedMessageBuilder<AvroRunTask>>()
        val slotSchema = slot<AvroSchema<AvroRunTask>>()
        val slotTopic = slot<String>()
        every { context.newOutputMessage<AvroRunTask>(capture(slotTopic), capture(slotSchema)) } returns builder
        every { builder.value(any()) } returns builder
        every { builder.send() } returns mockk<MessageId>()
        // given
        val msg = TestFactory.get(RunTask::class)
        // when
        TaskDispatcher(context).dispatch(msg)
        // then
        verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
        slotTopic.captured shouldBe Topic.TASK_ATTEMPTS.get(msg.taskName.name)
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroRunTask::class.java).avroSchema
        verify(exactly = 1) { builder.value(TaskAvroConverter.toAvro(msg)) }
        verify(exactly = 1) { builder.send() }
        confirmVerified(context)
        confirmVerified(builder)
    }

    include(dispatchShouldSendTaskMessageToTasksTopic(CancelTask::class))
    include(dispatchShouldSendTaskMessageToTasksTopic(DispatchTask::class))
    include(dispatchShouldSendTaskMessageToTasksTopic(RetryTask::class))
    include(dispatchShouldSendTaskMessageToTasksTopic(RetryTaskAttempt::class))
    include(dispatchShouldSendTaskMessageToTasksTopic(TaskAttemptCompleted::class))
    include(dispatchShouldSendTaskMessageToTasksTopic(TaskAttemptDispatched::class))
    include(dispatchShouldSendTaskMessageToTasksTopic(TaskAttemptFailed::class))
    include(dispatchShouldSendTaskMessageToTasksTopic(TaskAttemptStarted::class))
    include(dispatchShouldSendTaskMessageToTasksTopic(TaskCanceled::class))
    include(dispatchShouldSendTaskMessageToTasksTopic(TaskCompleted::class))
    include(dispatchShouldSendTaskMessageToTasksTopic(TaskDispatched::class))
})
