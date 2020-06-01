package com.zenaton.taskmanager.pulsar.dispatcher

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskmanager.engine.messages.AvroTaskEngineMessage
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
import com.zenaton.taskmanager.metrics.messages.AvroTaskMetricMessage
import com.zenaton.taskmanager.metrics.messages.TaskMetricMessage
import com.zenaton.taskmanager.metrics.messages.TaskStatusUpdated
import com.zenaton.taskmanager.pulsar.Topic
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.taskmanager.workers.AvroTaskWorkerMessage
import com.zenaton.taskmanager.workers.messages.RunTask
import com.zenaton.taskmanager.workers.messages.TaskWorkerMessage
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

class PulsarTaskDispatcherTests : StringSpec({
    include(shouldSendTaskMetricMessageToMetricsTopic(TaskStatusUpdated::class))

    include(shouldSendTaskWorkerMessageToTaskAttemptsTopic(RunTask::class))

    include(shouldSendTaskEngineMessageToTasksTopic(CancelTask::class))
    include(shouldSendTaskEngineMessageToTasksTopic(DispatchTask::class))
    include(shouldSendTaskEngineMessageToTasksTopic(RetryTask::class))
    include(shouldSendTaskEngineMessageToTasksTopic(RetryTaskAttempt::class))
    include(shouldSendTaskEngineMessageToTasksTopic(TaskAttemptCompleted::class))
    include(shouldSendTaskEngineMessageToTasksTopic(TaskAttemptDispatched::class))
    include(shouldSendTaskEngineMessageToTasksTopic(TaskAttemptFailed::class))
    include(shouldSendTaskEngineMessageToTasksTopic(TaskAttemptStarted::class))
    include(shouldSendTaskEngineMessageToTasksTopic(TaskCanceled::class))
    include(shouldSendTaskEngineMessageToTasksTopic(TaskCompleted::class))
    include(shouldSendTaskEngineMessageToTasksTopic(TaskDispatched::class))
})

fun <T : TaskEngineMessage> shouldSendTaskEngineMessageToTasksTopic(klass: KClass<T>) = stringSpec {
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
    PulsarTaskDispatcher(context).dispatch(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.ENGINE.get()
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroTaskEngineMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(TaskAvroConverter.toAvro(msg)) }
    verify(exactly = 1) { builder.key(msg.taskId.id) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}
fun <T : TaskMetricMessage> shouldSendTaskMetricMessageToMetricsTopic(klass: KClass<T>) = stringSpec {
    // mocking
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroTaskMetricMessage>>()
    val slotSchema = slot<AvroSchema<AvroTaskMetricMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroTaskMetricMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.key(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // given
    val msg = TestFactory.get(klass)
    // when
    PulsarTaskDispatcher(context).dispatch(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.METRICS.get()
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroTaskMetricMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(TaskAvroConverter.toAvro(msg)) }
    verify(exactly = 1) { builder.key(msg.taskName.name) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}

fun <T : TaskWorkerMessage> shouldSendTaskWorkerMessageToTaskAttemptsTopic(klass: KClass<T>) = stringSpec {
    // mocking
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroTaskWorkerMessage>>()
    val slotSchema = slot<AvroSchema<AvroTaskWorkerMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroTaskWorkerMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // given
    val msg = TestFactory.get(klass)
    // when
    PulsarTaskDispatcher(context).dispatch(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.WORKERS.get(msg.taskName.name)
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroTaskWorkerMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(TaskAvroConverter.toAvro(msg)) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}
