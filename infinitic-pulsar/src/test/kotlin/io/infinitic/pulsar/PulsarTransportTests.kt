/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.pulsar

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEngineMessage
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.pulsar.schemas.schemaDefinition
import io.infinitic.pulsar.topics.TaskExecutorTopic
import io.infinitic.pulsar.transport.PulsarTransport
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyAll
import org.apache.pulsar.client.api.TypedMessageBuilder
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context
import java.util.concurrent.CompletableFuture

class PulsarTransportTests : StringSpec({
    WorkflowEngineMessage::class.sealedSubclasses.forEach {
        include(shouldBeAbleToSendMessageToWorkflowEngineCommandsTopic(TestFactory.random(it)))
    }

    TaskEngineMessage::class.sealedSubclasses.forEach {
        include(shouldBeAbleToSendMessageToTaskEngineCommandsTopic(TestFactory.random(it)))
    }

    MonitoringPerNameEngineMessage::class.sealedSubclasses.forEach {
        include(shouldBeAbleToSendMessageToMonitoringPerNameTopic(TestFactory.random(it)))
    }

    MonitoringGlobalMessage::class.sealedSubclasses.forEach {
        include(shouldBeAbleToSendMessageToMonitoringGlobalTopic(TestFactory.random(it)))
    }

    TaskExecutorMessage::class.sealedSubclasses.forEach {
        include(shouldBeAbleToSendMessageToWorkerTopic(TestFactory.random(it)))
    }
})

private fun shouldBeAbleToSendMessageToWorkflowEngineCommandsTopic(msg: WorkflowEngineMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to WorkflowEngineCommands topic" {
        // given
        val context = context()
        val builder = mockk<TypedMessageBuilder<WorkflowEngineEnvelope>>()
        val slotSchema = slot<AvroSchema<WorkflowEngineEnvelope>>()
        every { context.newOutputMessage(any(), capture(slotSchema)) } returns builder
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.sendAsync() } returns CompletableFuture.completedFuture(mockk())
        // when
        PulsarTransport.from(context).sendToWorkflowEngineCommands(msg, 0F)
        // then
        verifyAll {
            context.newOutputMessage("workflow-engine-commands", slotSchema.captured)
        }
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<WorkflowEngineEnvelope>()).avroSchema
        confirmVerified(context)
        verifyAll {
            builder.value(WorkflowEngineEnvelope.from(msg))
            builder.key("${msg.workflowId}")
            builder.sendAsync()
        }
        confirmVerified(builder)
    }
}

private fun shouldBeAbleToSendMessageToTaskEngineCommandsTopic(msg: TaskEngineMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to TaskEngineCommands topic" {
        // given
        val context = context()
        val builder = mockk<TypedMessageBuilder<TaskEngineEnvelope>>()
        val slotSchema = slot<AvroSchema<TaskEngineEnvelope>>()
        every { context.newOutputMessage(any(), capture(slotSchema)) } returns builder
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.sendAsync() } returns CompletableFuture.completedFuture(mockk())
        // when
        PulsarTransport.from(context).sendToTaskEngineCommands(msg, 0F)
        // then
        verifyAll {
            context.newOutputMessage("task-engine-commands", slotSchema.captured)
        }
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<TaskEngineEnvelope>()).avroSchema
        confirmVerified(context)
        verifyAll {
            builder.value(TaskEngineEnvelope.from(msg))
            builder.key("${msg.taskId}")
            builder.sendAsync()
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
        every { context.newOutputMessage(any(), capture(slotSchema)) } returns builder
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.sendAsync() } returns CompletableFuture.completedFuture(mockk())
        // when
        PulsarTransport.from(context).sendToMonitoringPerNameEngine(msg)
        // then
        verifyAll {
            context.newOutputMessage("monitoring-per-name", slotSchema.captured)
        }
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<MonitoringPerNameEnvelope>()).avroSchema
        confirmVerified(context)
        verifyAll {
            builder.value(MonitoringPerNameEnvelope.from(msg))
            builder.key("${msg.taskName}")
            builder.sendAsync()
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
        every { context.newOutputMessage(capture(slotTopic), capture(slotSchema)) } returns builder
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.sendAsync() } returns CompletableFuture.completedFuture(mockk())
        // when
        PulsarTransport.from(context).sendToMonitoringGlobalEngine(msg)
        // then
        verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
        slotTopic.captured shouldBe "monitoring-global"
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<MonitoringGlobalEnvelope>()).avroSchema
        verify(exactly = 1) { builder.value(MonitoringGlobalEnvelope.from(msg)) }
        verify(exactly = 1) { builder.sendAsync() }
        confirmVerified(context)
        confirmVerified(builder)
    }
}

private fun shouldBeAbleToSendMessageToWorkerTopic(msg: TaskExecutorMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to Worker topic" {
        val context = context()
        val builder = mockk<TypedMessageBuilder<TaskExecutorEnvelope>>()
        val slotSchema = slot<AvroSchema<TaskExecutorEnvelope>>()
        val slotTopic = slot<String>()
        every { context.newOutputMessage(capture(slotTopic), capture(slotSchema)) } returns builder
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.sendAsync() } returns CompletableFuture.completedFuture(mockk())
        // when
        PulsarTransport.from(context).sendToExecutors(msg)
        // then
        verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
        slotTopic.captured shouldBe TaskExecutorTopic.name("${msg.taskName}")
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<TaskExecutorEnvelope>()).avroSchema
        verify(exactly = 1) { builder.value(TaskExecutorEnvelope.from(msg)) }
        verify(exactly = 1) { builder.key("${msg.taskName}") }
        verify(exactly = 1) { builder.sendAsync() }
        confirmVerified(context)
        confirmVerified(builder)
    }
}

fun context(): Context {
    val context = mockk<Context>()
    every { context.logger } returns mockk(relaxed = true)

    return context
}
