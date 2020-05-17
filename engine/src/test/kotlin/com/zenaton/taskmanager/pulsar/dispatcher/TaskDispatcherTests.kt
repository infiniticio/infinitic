package com.zenaton.taskmanager.pulsar.dispatcher

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskmanager.messages.AvroRunTask
import com.zenaton.taskmanager.messages.AvroTaskMessage
import com.zenaton.taskmanager.messages.RunTask
import com.zenaton.taskmanager.messages.commands.CancelTask
import com.zenaton.taskmanager.messages.commands.DispatchTask
import com.zenaton.taskmanager.messages.commands.RetryTask
import com.zenaton.taskmanager.messages.commands.RetryTaskAttempt
import com.zenaton.taskmanager.messages.events.TaskAttemptCompleted
import com.zenaton.taskmanager.messages.events.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.events.TaskAttemptFailed
import com.zenaton.taskmanager.messages.events.TaskAttemptStarted
import com.zenaton.taskmanager.messages.events.TaskCanceled
import com.zenaton.taskmanager.messages.interfaces.TaskMessageInterface
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

fun <T : TaskMessageInterface> dispatchShouldSendTaskMessageToTasksTopic(klass: KClass<T>) = stringSpec {
    // mocking
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroTaskMessage>>()
    val slotSchema = slot<AvroSchema<AvroTaskMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroTaskMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.key(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // given
    val msg = TestFactory.get(klass)
    // when
    TaskDispatcher.dispatch(context, msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.TASKS.get()
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroTaskMessage::class.java).avroSchema
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
        TaskDispatcher.dispatch(context, msg)
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
})
