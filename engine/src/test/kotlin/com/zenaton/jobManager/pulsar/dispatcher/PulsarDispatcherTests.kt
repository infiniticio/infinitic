package com.zenaton.jobManager.pulsar.dispatcher

import com.zenaton.commons.utils.TestFactory
import com.zenaton.jobManager.messages.AvroForEngineMessage
import com.zenaton.jobManager.messages.AvroForMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.AvroForWorkerMessage
import com.zenaton.jobManager.messages.CancelJob
import com.zenaton.jobManager.messages.DispatchJob
import com.zenaton.jobManager.messages.JobAttemptCompleted
import com.zenaton.jobManager.messages.JobAttemptDispatched
import com.zenaton.jobManager.messages.JobAttemptFailed
import com.zenaton.jobManager.messages.JobAttemptStarted
import com.zenaton.jobManager.messages.JobCanceled
import com.zenaton.jobManager.messages.JobCompleted
import com.zenaton.jobManager.messages.JobDispatched
import com.zenaton.jobManager.messages.JobStatusUpdated
import com.zenaton.jobManager.messages.RetryJob
import com.zenaton.jobManager.messages.RetryJobAttempt
import com.zenaton.jobManager.messages.RunJob
import com.zenaton.jobManager.messages.interfaces.ForEngineMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.interfaces.ForWorkerMessage
import com.zenaton.jobManager.pulsar.Topic
import com.zenaton.jobManager.pulsar.avro.AvroConverter
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

class PulsarDispatcherTests : StringSpec({
    include(shouldSendMessageToMonitoringPerNameTopic(JobStatusUpdated::class))

    include(shouldSendMessageToMonitoringPerInstanceTopic(JobAttemptDispatched::class))
    include(shouldSendMessageToMonitoringPerInstanceTopic(JobCanceled::class))
    include(shouldSendMessageToMonitoringPerInstanceTopic(JobCompleted::class))
    include(shouldSendMessageToMonitoringPerInstanceTopic(JobDispatched::class))

    include(shouldSendMessageToWorkersTopic(RunJob::class))

    include(shouldSendMessageToEngineTopic(CancelJob::class))
    include(shouldSendMessageToEngineTopic(DispatchJob::class))
    include(shouldSendMessageToEngineTopic(RetryJob::class))
    include(shouldSendMessageToEngineTopic(RetryJobAttempt::class))
    include(shouldSendMessageToEngineTopic(JobAttemptCompleted::class))
    include(shouldSendMessageToEngineTopic(JobAttemptFailed::class))
    include(shouldSendMessageToEngineTopic(JobAttemptStarted::class))
})

fun <T : ForEngineMessage> shouldSendMessageToEngineTopic(klass: KClass<T>) = stringSpec {
    // mocking
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroForEngineMessage>>()
    val slotSchema = slot<AvroSchema<AvroForEngineMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroForEngineMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.key(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // given
    val msg = TestFactory.get(klass)
    // when
    PulsarDispatcher(context).toEngine(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.ENGINE.get()
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroForEngineMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(AvroConverter.toAvro(msg)) }
    verify(exactly = 1) { builder.key(msg.jobId.id) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}
fun <T : ForMonitoringPerNameMessage> shouldSendMessageToMonitoringPerNameTopic(klass: KClass<T>) = stringSpec {
    // mocking
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroForMonitoringPerNameMessage>>()
    val slotSchema = slot<AvroSchema<AvroForMonitoringPerNameMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroForMonitoringPerNameMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.key(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // given
    val msg = TestFactory.get(klass)
    // when
    PulsarDispatcher(context).toMonitoringPerName(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.MONITORING_PER_NAME.get()
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroForMonitoringPerNameMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(AvroConverter.toAvro(msg)) }
    verify(exactly = 1) { builder.key(msg.jobName.name) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}

fun <T : ForMonitoringPerInstanceMessage> shouldSendMessageToMonitoringPerInstanceTopic(klass: KClass<T>) = stringSpec {
    // mocking
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroForMonitoringPerInstanceMessage>>()
    val slotSchema = slot<AvroSchema<AvroForMonitoringPerInstanceMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroForMonitoringPerInstanceMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.key(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // given
    val msg = TestFactory.get(klass)
    // when
    PulsarDispatcher(context).toMonitoringPerInstance(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.MONITORING_PER_INSTANCE.get()
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroForMonitoringPerInstanceMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(AvroConverter.toAvro(msg)) }
    verify(exactly = 1) { builder.key(msg.jobId.id) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}

fun <T : ForWorkerMessage> shouldSendMessageToWorkersTopic(klass: KClass<T>) = stringSpec {
    // mocking
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroForWorkerMessage>>()
    val slotSchema = slot<AvroSchema<AvroForWorkerMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroForWorkerMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // given
    val msg = TestFactory.get(klass)
    // when
    PulsarDispatcher(context).toWorkers(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.WORKERS.get(msg.jobName.name)
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroForWorkerMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(AvroConverter.toAvro(msg)) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}
