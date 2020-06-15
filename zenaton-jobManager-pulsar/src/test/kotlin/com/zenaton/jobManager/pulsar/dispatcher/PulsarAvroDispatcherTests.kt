package com.zenaton.jobManager.pulsar.dispatcher

import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.messages.Message
import com.zenaton.jobManager.messages.envelopes.AvroForEngineMessage
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.envelopes.AvroForWorkerMessage
import com.zenaton.jobManager.messages.envelopes.ForEngineMessage
import com.zenaton.jobManager.messages.envelopes.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.envelopes.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.envelopes.ForWorkerMessage
import com.zenaton.jobManager.pulsar.utils.TestFactory
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyAll
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.TypedMessageBuilder
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context
import java.util.Optional

class PulsarAvroDispatcherTests : StringSpec({

    "it should be possible to dispatch messages" {
        Message::class.sealedSubclasses.forEach {
            shouldNotThrowAny {
                val msg = TestFactory.get(it)
                if (msg is ForWorkerMessage) {
                    shouldSendMessageToWorkersTopic(AvroConverter.toWorkers(msg))
                }
                if (msg is ForEngineMessage) {
                    shouldSendMessageToEngineTopic(AvroConverter.toEngine(msg))
                }
                if (msg is ForMonitoringPerNameMessage) {
                    shouldSendMessageToMonitoringPerNameTopic(AvroConverter.toMonitoringPerName(msg))
                }
                if (msg is ForMonitoringGlobalMessage) {
                    shouldSendMessageToMonitoringGlobalTopic(AvroConverter.toMonitoringGlobal(msg))
                }
            }
        }
    }
})

fun shouldSendMessageToEngineTopic(msg: AvroForEngineMessage) = stringSpec {
    // given
    val prefix = TestFactory.get(String::class)
    val context = context(prefix)
    val builder = mockk<TypedMessageBuilder<AvroForEngineMessage>>()
    val slotSchema = slot<AvroSchema<AvroForEngineMessage>>()
    every { context.newOutputMessage<AvroForEngineMessage>(any(), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.key(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // when
    PulsarAvroDispatcher(context).toEngine(msg)
    // then
    verifyAll {
        context.newOutputMessage(Topic.ENGINE.get(prefix), slotSchema.captured)
    }
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroForEngineMessage::class.java).avroSchema
    confirmVerified(context)
    verifyAll {
        builder.value(msg)
        builder.key(msg.jobId)
        builder.send()
    }
    confirmVerified(builder)
}
fun shouldSendMessageToMonitoringPerNameTopic(msg: AvroForMonitoringPerNameMessage) = stringSpec {
    // given
    val prefix = TestFactory.get(String::class)
    val context = context(prefix)
    val builder = mockk<TypedMessageBuilder<AvroForMonitoringPerNameMessage>>()
    val slotSchema = slot<AvroSchema<AvroForMonitoringPerNameMessage>>()
    every { context.newOutputMessage<AvroForMonitoringPerNameMessage>(any(), capture(slotSchema)) } returns builder
    every { builder.value(msg) } returns builder
    every { builder.key(msg.jobName) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // when
    PulsarAvroDispatcher(context).toMonitoringPerName(msg)
    // then
    verifyAll {
        context.newOutputMessage(Topic.MONITORING_PER_NAME.get(prefix), slotSchema.captured)
    }
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroForMonitoringPerNameMessage::class.java).avroSchema
    confirmVerified(context)
    verifyAll {
        builder.value(msg)
        builder.key(msg.jobName)
        builder.send()
    }
    confirmVerified(builder)
}

fun shouldSendMessageToMonitoringGlobalTopic(msg: AvroForMonitoringGlobalMessage) = stringSpec {
    // given
    val prefix = TestFactory.get(String::class)
    val context = context(prefix)
    val builder = mockk<TypedMessageBuilder<AvroForMonitoringGlobalMessage>>()
    val slotSchema = slot<AvroSchema<AvroForMonitoringGlobalMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroForMonitoringGlobalMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.key(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // when
    PulsarAvroDispatcher(context).toMonitoringGlobal(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.MONITORING_GLOBAL.get(prefix)
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroForMonitoringGlobalMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(msg) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}

fun shouldSendMessageToWorkersTopic(msg: AvroForWorkerMessage) = stringSpec {
    // given
    val prefix = TestFactory.get(String::class)
    val context = context(prefix)
    val builder = mockk<TypedMessageBuilder<AvroForWorkerMessage>>()
    val slotSchema = slot<AvroSchema<AvroForWorkerMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroForWorkerMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // when
    PulsarAvroDispatcher(context).toWorkers(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.WORKERS.get(prefix, msg.jobName)
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroForWorkerMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(msg) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}

fun context(prefix: String): Context {
    val context = mockk<Context>()
    val optional = mockk<Optional<Any>>()
    every { context.getUserConfigValue("topicPrefix") } returns optional
    every { optional.isPresent } returns true
    every { optional.get() } returns prefix
    return context
}
