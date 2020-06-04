package com.zenaton.jobManager.pulsar.dispatcher

import com.zenaton.jobManager.messages.AvroForEngineMessage
import com.zenaton.jobManager.messages.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.AvroForMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.AvroForWorkerMessage
import com.zenaton.jobManager.messages.Message
import com.zenaton.jobManager.messages.interfaces.ForEngineMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.interfaces.ForWorkerMessage
import com.zenaton.jobManager.pulsar.Topic
import com.zenaton.jobManager.pulsar.avro.AvroConverter
import com.zenaton.jobManager.utils.TestFactory
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.TypedMessageBuilder
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

class PulsarDispatcherTests : StringSpec({

    "it should be possible to dispatch messages" {
        Message::class.sealedSubclasses.forEach {
            shouldNotThrowAny {
                val msg = TestFactory.get(it)
                if (msg is ForWorkerMessage) {
                    shouldSendMessageToWorkersTopic(msg)
                }
                if (msg is ForEngineMessage) {
                    shouldSendMessageToEngineTopic(msg)
                }
                if (msg is ForMonitoringPerInstanceMessage) {
                    shouldSendMessageToMonitoringPerInstanceTopic(msg)
                }
                if (msg is ForMonitoringPerNameMessage) {
                    shouldSendMessageToMonitoringPerNameTopic(msg)
                }
                if (msg is ForMonitoringGlobalMessage) {
                    shouldSendMessageToMonitoringGlobalTopic(msg)
                }
            }
        }
    }
})

fun shouldSendMessageToEngineTopic(msg: ForEngineMessage) = stringSpec {
    // given
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroForEngineMessage>>()
    val slotSchema = slot<AvroSchema<AvroForEngineMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroForEngineMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.key(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // when
    PulsarDispatcher(context).toEngine(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.ENGINE.get()
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroForEngineMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(AvroConverter.toAvroForEngineMessage(msg)) }
    verify(exactly = 1) { builder.key(msg.jobId.id) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}
fun shouldSendMessageToMonitoringPerNameTopic(msg: ForMonitoringPerNameMessage) = stringSpec {
    // given
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroForMonitoringPerNameMessage>>()
    val slotSchema = slot<AvroSchema<AvroForMonitoringPerNameMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroForMonitoringPerNameMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.key(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // when
    PulsarDispatcher(context).toMonitoringPerName(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.MONITORING_PER_NAME.get()
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroForMonitoringPerNameMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(AvroConverter.toAvroForMonitoringPerNameMessage(msg)) }
    verify(exactly = 1) { builder.key(msg.jobName.name) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}

fun shouldSendMessageToMonitoringPerInstanceTopic(msg: ForMonitoringPerInstanceMessage) = stringSpec {
    // given
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroForMonitoringPerInstanceMessage>>()
    val slotSchema = slot<AvroSchema<AvroForMonitoringPerInstanceMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroForMonitoringPerInstanceMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.key(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // when
    PulsarDispatcher(context).toMonitoringPerInstance(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.MONITORING_PER_INSTANCE.get()
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroForMonitoringPerInstanceMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(AvroConverter.toAvroForMonitoringPerInstanceMessage(msg)) }
    verify(exactly = 1) { builder.key(msg.jobId.id) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}

fun shouldSendMessageToMonitoringGlobalTopic(msg: ForMonitoringGlobalMessage) = stringSpec {
    // given
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroForMonitoringGlobalMessage>>()
    val slotSchema = slot<AvroSchema<AvroForMonitoringGlobalMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroForMonitoringGlobalMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.key(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // when
    PulsarDispatcher(context).toMonitoringGlobal(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.MONITORING_GLOBAL.get()
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroForMonitoringGlobalMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(AvroConverter.toAvroForMonitoringGlobalMessage(msg)) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}

fun shouldSendMessageToWorkersTopic(msg: ForWorkerMessage) = stringSpec {
    // given
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroForWorkerMessage>>()
    val slotSchema = slot<AvroSchema<AvroForWorkerMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroForWorkerMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // when
    PulsarDispatcher(context).toWorkers(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.WORKERS.get(msg.jobName.name)
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroForWorkerMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(AvroConverter.toAvroForWorkerMessage(msg)) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}
