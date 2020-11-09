// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.messaging.pulsar

import io.infinitic.common.serDe.avro.AvroConverter
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForWorker
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.messages.monitoringGlobalMessages.MonitoringGlobalMessage
import io.infinitic.common.tasks.messages.monitoringPerNameMessages.MonitoringPerNameEngineMessage
import io.infinitic.common.tasks.messages.taskEngineMessages.TaskEngineMessage
import io.infinitic.common.tasks.messages.workerMessages.WorkerMessage
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

class PulsarTransportTests : StringSpec({
    TaskEngineMessage::class.sealedSubclasses.forEach {
        include(shouldBeAbleToSendMessageToEngineTopic(TestFactory.random(it)))
    }

    MonitoringPerNameEngineMessage::class.sealedSubclasses.forEach {
        include(shouldBeAbleToSendMessageToMonitoringPerNameTopic(TestFactory.random(it)))
    }

    MonitoringGlobalMessage::class.sealedSubclasses.forEach {
        include(shouldBeAbleToSendMessageToMonitoringGlobalTopic(TestFactory.random(it)))
    }

    WorkerMessage::class.sealedSubclasses.forEach {
        include(shouldBeAbleToSendMessageToWorkerTopic(TestFactory.random(it)))
    }
})

private fun shouldBeAbleToSendMessageToEngineTopic(msg: TaskEngineMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to TaskEngine topic" {
        val avro = AvroConverter.toTaskEngine(msg)
        // given
        val context = context()
        val builder = mockk<TypedMessageBuilder<AvroEnvelopeForTaskEngine>>()
        val slotSchema = slot<AvroSchema<AvroEnvelopeForTaskEngine>>()
        every { context.newOutputMessage<AvroEnvelopeForTaskEngine>(any(), capture(slotSchema)) } returns builder
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.send() } returns mockk<MessageId>()
        // when
        PulsarTransport.forPulsarFunctionContext(context).toTaskEngine(avro)
        // then
        verifyAll {
            context.newOutputMessage(Topic.TASK_ENGINE.get(), slotSchema.captured)
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

private fun shouldBeAbleToSendMessageToMonitoringPerNameTopic(msg: MonitoringPerNameEngineMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to MonitoringPerName topic " {
        val avro = AvroConverter.toMonitoringPerName(msg)
        // given
        val context = context()
        val builder = mockk<TypedMessageBuilder<AvroEnvelopeForMonitoringPerName>>()
        val slotSchema = slot<AvroSchema<AvroEnvelopeForMonitoringPerName>>()
        every { context.newOutputMessage<AvroEnvelopeForMonitoringPerName>(any(), capture(slotSchema)) } returns builder
        every { builder.value(avro) } returns builder
        every { builder.key(avro.taskName) } returns builder
        every { builder.send() } returns mockk<MessageId>()
        // when
        PulsarTransport.forPulsarFunctionContext(context).toMonitoringPerName(avro)
        // then
        verifyAll {
            context.newOutputMessage(Topic.MONITORING_PER_NAME.get(), slotSchema.captured)
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

private fun shouldBeAbleToSendMessageToMonitoringGlobalTopic(msg: MonitoringGlobalMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to MonitoringGlobal topic " {
        val avro = AvroConverter.toMonitoringGlobal(msg)
        // given
        val context = context()
        val builder = mockk<TypedMessageBuilder<AvroEnvelopeForMonitoringGlobal>>()
        val slotSchema = slot<AvroSchema<AvroEnvelopeForMonitoringGlobal>>()
        val slotTopic = slot<String>()
        every { context.newOutputMessage<AvroEnvelopeForMonitoringGlobal>(capture(slotTopic), capture(slotSchema)) } returns builder
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.send() } returns mockk<MessageId>()
        // when
        PulsarTransport.forPulsarFunctionContext(context).toMonitoringGlobal(avro)
        // then
        verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
        slotTopic.captured shouldBe Topic.MONITORING_GLOBAL.get()
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroEnvelopeForMonitoringGlobal::class.java).avroSchema
        verify(exactly = 1) { builder.value(avro) }
        verify(exactly = 1) { builder.send() }
        confirmVerified(context)
        confirmVerified(builder)
    }
}

private fun shouldBeAbleToSendMessageToWorkerTopic(msg: WorkerMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to Worker topic" {
        val avro = AvroConverter.toWorkers(msg) // given
        val context = context()
        val builder = mockk<TypedMessageBuilder<AvroEnvelopeForWorker>>()
        val slotSchema = slot<AvroSchema<AvroEnvelopeForWorker>>()
        val slotTopic = slot<String>()
        every { context.newOutputMessage<AvroEnvelopeForWorker>(capture(slotTopic), capture(slotSchema)) } returns builder
        every { builder.value(any()) } returns builder
        every { builder.send() } returns mockk<MessageId>()
        // when
        PulsarTransport.forPulsarFunctionContext(context).toWorkers(avro)
        // then
        verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
        slotTopic.captured shouldBe Topic.WORKERS.get(avro.taskName)
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroEnvelopeForWorker::class.java).avroSchema
        verify(exactly = 1) { builder.value(avro) }
        verify(exactly = 1) { builder.send() }
        confirmVerified(context)
        confirmVerified(builder)
    }
}

fun context(): Context {
    val context = mockk<Context>()
    val optional = mockk<Optional<Any>>()
    every { context.logger } returns mockk<Logger>(relaxed = true)

    return context
}
