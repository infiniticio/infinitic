package com.zenaton.taskManager.pulsar.dispatcher

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskManager.engine.messages.AvroTaskEngineMessage
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
import com.zenaton.taskManager.metrics.messages.AvroMonitoringPerNameMessage
import com.zenaton.taskManager.monitoring.perName.MonitoringPerNameMessage
import com.zenaton.taskManager.monitoring.perName.TaskStatusUpdated
import com.zenaton.taskManager.pulsar.Topic
import com.zenaton.taskManager.pulsar.avro.AvroConverter
import com.zenaton.taskManager.workers.AvroTaskWorkerMessage
import com.zenaton.taskManager.workers.RunTask
import com.zenaton.taskManager.workers.WorkerMessage
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

fun <T : EngineMessage> shouldSendTaskEngineMessageToTasksTopic(klass: KClass<T>) = stringSpec {
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
    PulsarDispatcher(context).dispatch(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.ENGINE.get()
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroTaskEngineMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(AvroConverter.toAvro(msg)) }
    verify(exactly = 1) { builder.key(msg.taskId.id) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}
fun <T : MonitoringPerNameMessage> shouldSendTaskMetricMessageToMetricsTopic(klass: KClass<T>) = stringSpec {
    // mocking
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroMonitoringPerNameMessage>>()
    val slotSchema = slot<AvroSchema<AvroMonitoringPerNameMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroMonitoringPerNameMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.key(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // given
    val msg = TestFactory.get(klass)
    // when
    PulsarDispatcher(context).dispatch(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.MONITORING_PER_NAME.get()
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroMonitoringPerNameMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(AvroConverter.toAvro(msg)) }
    verify(exactly = 1) { builder.key(msg.taskName.name) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}

fun <T : WorkerMessage> shouldSendTaskWorkerMessageToTaskAttemptsTopic(klass: KClass<T>) = stringSpec {
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
    PulsarDispatcher(context).dispatch(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.WORKERS.get(msg.taskName.name)
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroTaskWorkerMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(AvroConverter.toAvro(msg)) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}
