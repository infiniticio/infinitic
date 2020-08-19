package io.infinitic.taskManager.engine.pulsar.dispatcher

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage
import io.infinitic.taskManager.common.messages.ForMonitoringGlobalMessage
import io.infinitic.taskManager.common.messages.ForMonitoringPerNameMessage
import io.infinitic.taskManager.common.messages.ForWorkerMessage
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForWorker
import io.infinitic.taskManager.engine.pulsar.utils.TestFactory
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
import org.slf4j.Logger
import java.util.Optional

class PulsarAvroDispatcherTests : StringSpec({
    ForTaskEngineMessage::class.sealedSubclasses.forEach {
        include(shouldBeAbleToSendMessageToEngineTopic(TestFactory.random(it)))
    }

    ForMonitoringPerNameMessage::class.sealedSubclasses.forEach {
        include(shouldBeAbleToSendMessageToMonitoringPerNameTopic(TestFactory.random(it)))
    }

    ForMonitoringGlobalMessage::class.sealedSubclasses.forEach {
        include(shouldBeAbleToSendMessageToMonitoringGlobalTopic(TestFactory.random(it)))
    }

    ForWorkerMessage::class.sealedSubclasses.forEach {
        include(shouldBeAbleToSendMessageToWorkerTopic(TestFactory.random(it)))
    }
})

private fun shouldBeAbleToSendMessageToEngineTopic(msg: ForTaskEngineMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to TaskEngine topic" {
        val avro = AvroConverter.toTaskEngine(msg)
        // given
        val prefix = TestFactory.random(String::class)
        val context = context(prefix)
        val builder = mockk<TypedMessageBuilder<AvroEnvelopeForTaskEngine>>()
        val slotSchema = slot<AvroSchema<AvroEnvelopeForTaskEngine>>()
        every { context.newOutputMessage<AvroEnvelopeForTaskEngine>(any(), capture(slotSchema)) } returns builder
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.send() } returns mockk<MessageId>()
        // when
        PulsarAvroDispatcher(context).toTaskEngine(avro)
        // then
        verifyAll {
            context.newOutputMessage(Topic.TASK_ENGINE.get(prefix), slotSchema.captured)
            context.logger
            context.getUserConfigValue("topicPrefix")
        }
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroEnvelopeForTaskEngine::class.java).avroSchema
        confirmVerified(context)
        verifyAll {
            builder.value(avro)
            builder.key(avro.taskId)
            builder.send()
        }
        confirmVerified(builder)
    }
}

private fun shouldBeAbleToSendMessageToMonitoringPerNameTopic(msg: ForMonitoringPerNameMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to MonitoringPerName topic " {
        val avro = AvroConverter.toMonitoringPerName(msg)
        // given
        val prefix = TestFactory.random(String::class)
        val context = context(prefix)
        val builder = mockk<TypedMessageBuilder<AvroEnvelopeForMonitoringPerName>>()
        val slotSchema = slot<AvroSchema<AvroEnvelopeForMonitoringPerName>>()
        every { context.newOutputMessage<AvroEnvelopeForMonitoringPerName>(any(), capture(slotSchema)) } returns builder
        every { builder.value(avro) } returns builder
        every { builder.key(avro.taskName) } returns builder
        every { builder.send() } returns mockk<MessageId>()
        // when
        PulsarAvroDispatcher(context).toMonitoringPerName(avro)
        // then
        verifyAll {
            context.newOutputMessage(Topic.MONITORING_PER_NAME.get(prefix), slotSchema.captured)
            context.logger
            context.getUserConfigValue("topicPrefix")
        }
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroEnvelopeForMonitoringPerName::class.java).avroSchema
        confirmVerified(context)
        verifyAll {
            builder.value(avro)
            builder.key(avro.taskName)
            builder.send()
        }
        confirmVerified(builder)
    }
}

private fun shouldBeAbleToSendMessageToMonitoringGlobalTopic(msg: ForMonitoringGlobalMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to MonitoringGlobal topic " {
        val avro = AvroConverter.toMonitoringGlobal(msg)
        // given
        val prefix = TestFactory.random(String::class)
        val context = context(prefix)
        val builder = mockk<TypedMessageBuilder<AvroEnvelopeForMonitoringGlobal>>()
        val slotSchema = slot<AvroSchema<AvroEnvelopeForMonitoringGlobal>>()
        val slotTopic = slot<String>()
        every { context.newOutputMessage<AvroEnvelopeForMonitoringGlobal>(capture(slotTopic), capture(slotSchema)) } returns builder
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.send() } returns mockk<MessageId>()
        // when
        PulsarAvroDispatcher(context).toMonitoringGlobal(avro)
        // then
        verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
        verify {
            context.logger
            context.getUserConfigValue("topicPrefix")
        }
        slotTopic.captured shouldBe Topic.MONITORING_GLOBAL.get(prefix)
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroEnvelopeForMonitoringGlobal::class.java).avroSchema
        verify(exactly = 1) { builder.value(avro) }
        verify(exactly = 1) { builder.send() }
        confirmVerified(context)
        confirmVerified(builder)
    }
}

private fun shouldBeAbleToSendMessageToWorkerTopic(msg: ForWorkerMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to Worker topic" {
        val avro = AvroConverter.toWorkers(msg) // given
        val prefix = TestFactory.random(String::class)
        val context = context(prefix)
        val builder = mockk<TypedMessageBuilder<AvroEnvelopeForWorker>>()
        val slotSchema = slot<AvroSchema<AvroEnvelopeForWorker>>()
        val slotTopic = slot<String>()
        every { context.newOutputMessage<AvroEnvelopeForWorker>(capture(slotTopic), capture(slotSchema)) } returns builder
        every { builder.value(any()) } returns builder
        every { builder.send() } returns mockk<MessageId>()
        // when
        PulsarAvroDispatcher(context).toWorkers(avro)
        // then
        verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
        verify {
            context.logger
            context.getUserConfigValue("topicPrefix")
        }
        slotTopic.captured shouldBe Topic.WORKERS.get(prefix, avro.taskName)
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroEnvelopeForWorker::class.java).avroSchema
        verify(exactly = 1) { builder.value(avro) }
        verify(exactly = 1) { builder.send() }
        confirmVerified(context)
        confirmVerified(builder)
    }
}

fun context(prefix: String): Context {
    val context = mockk<Context>()
    val optional = mockk<Optional<Any>>()
    every { context.logger } returns mockk<Logger>(relaxed = true)
    every { context.getUserConfigValue("topicPrefix") } returns optional
    every { optional.isPresent } returns true
    every { optional.get() } returns prefix
    return context
}
