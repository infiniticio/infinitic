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

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.monitoringGlobal.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoringGlobal.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoringPerName.messages.MonitoringPerNameEngineMessage
import io.infinitic.common.monitoringPerName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.tasks.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.messages.TaskEngineMessage
import io.infinitic.common.workers.messages.WorkerEnvelope
import io.infinitic.common.workers.messages.WorkerMessage
import io.infinitic.messaging.pulsar.extensions.messageBuilder
import io.infinitic.messaging.pulsar.schemas.schemaDefinition
import io.infinitic.messaging.pulsar.senders.getSendToMonitoringGlobal
import io.infinitic.messaging.pulsar.senders.getSendToMonitoringPerName
import io.infinitic.messaging.pulsar.senders.getSendToTaskEngine
import io.infinitic.messaging.pulsar.senders.getSendToWorkers
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
        include(shouldBeAbleToSendMessageToTaskEngineTopic(TestFactory.random(it)))
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

private fun shouldBeAbleToSendMessageToTaskEngineTopic(msg: TaskEngineMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to TaskEngine topic" {
        // given
        val context = context()
        val builder = mockk<TypedMessageBuilder<TaskEngineEnvelope>>()
        val slotSchema = slot<AvroSchema<TaskEngineEnvelope>>()
        every { context.newOutputMessage<TaskEngineEnvelope>(any(), capture(slotSchema)) } returns builder
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.send() } returns mockk<MessageId>()
        // when
        getSendToTaskEngine(context.messageBuilder())(msg, 0F)
        // then
        verifyAll {
            context.newOutputMessage(Topic.TASK_ENGINE.get(), slotSchema.captured)
        }
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<TaskEngineEnvelope>()).avroSchema
        confirmVerified(context)
        verifyAll {
            builder.value(TaskEngineEnvelope.from(msg))
            builder.key("${msg.taskId}")
            builder.send()
        }
        confirmVerified(builder)
    }
}

private fun shouldBeAbleToSendMessageToMonitoringPerNameTopic(msg: MonitoringPerNameEngineMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to MonitoringPerName topic " {
        // given
        val context = context()
        val builder = mockk<TypedMessageBuilder<MonitoringPerNameEnvelope>>()
        val slotSchema = slot<AvroSchema<MonitoringPerNameEnvelope>>()
        every { context.newOutputMessage<MonitoringPerNameEnvelope>(any(), capture(slotSchema)) } returns builder
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.send() } returns mockk<MessageId>()
        // when
        getSendToMonitoringPerName(context.messageBuilder())(msg)
        // then
        verifyAll {
            context.newOutputMessage(Topic.MONITORING_PER_NAME.get(), slotSchema.captured)
        }
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<MonitoringPerNameEnvelope>()).avroSchema
        confirmVerified(context)
        verifyAll {
            builder.value(MonitoringPerNameEnvelope.from(msg))
            builder.key("${msg.taskName}")
            builder.send()
        }
        confirmVerified(builder)
    }
}

private fun shouldBeAbleToSendMessageToMonitoringGlobalTopic(msg: MonitoringGlobalMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to MonitoringGlobal topic " {
        // given
        val context = context()
        val builder = mockk<TypedMessageBuilder<MonitoringGlobalEnvelope>>()
        val slotSchema = slot<AvroSchema<MonitoringGlobalEnvelope>>()
        val slotTopic = slot<String>()
        every { context.newOutputMessage<MonitoringGlobalEnvelope>(capture(slotTopic), capture(slotSchema)) } returns builder
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.send() } returns mockk<MessageId>()
        // when
        getSendToMonitoringGlobal(context.messageBuilder())(msg)
        // then
        verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
        slotTopic.captured shouldBe Topic.MONITORING_GLOBAL.get()
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<MonitoringGlobalEnvelope>()).avroSchema
        verify(exactly = 1) { builder.value(MonitoringGlobalEnvelope.from(msg)) }
        verify(exactly = 1) { builder.send() }
        confirmVerified(context)
        confirmVerified(builder)
    }
}

private fun shouldBeAbleToSendMessageToWorkerTopic(msg: WorkerMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to Worker topic" {
        val context = context()
        val builder = mockk<TypedMessageBuilder<WorkerEnvelope>>()
        val slotSchema = slot<AvroSchema<WorkerEnvelope>>()
        val slotTopic = slot<String>()
        every { context.newOutputMessage<WorkerEnvelope>(capture(slotTopic), capture(slotSchema)) } returns builder
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.send() } returns mockk<MessageId>()
        // when
        getSendToWorkers(context.messageBuilder())(msg)
        // then
        verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
        slotTopic.captured shouldBe Topic.WORKERS.get("${msg.taskName}")
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<WorkerEnvelope>()).avroSchema
        verify(exactly = 1) { builder.value(WorkerEnvelope.from(msg)) }
        verify(exactly = 1) { builder.key("${msg.taskName}") }
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
