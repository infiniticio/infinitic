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

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.metrics.global.messages.MetricsGlobalEnvelope
import io.infinitic.common.metrics.global.messages.MetricsGlobalMessage
import io.infinitic.common.metrics.perName.messages.MetricsPerNameEnvelope
import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.pulsar.schemas.schemaDefinition
import io.infinitic.pulsar.topics.TopicType
import io.infinitic.pulsar.transport.PulsarOutput
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

    MetricsPerNameMessage::class.sealedSubclasses.forEach {
        include(shouldBeAbleToSendMessageToMetricsPerNameTopic(TestFactory.random(it)))
    }

    MetricsGlobalMessage::class.sealedSubclasses.forEach {
        include(shouldBeAbleToSendMessageToMetricsGlobalTopic(TestFactory.random(it)))
    }

    TaskExecutorMessage::class.sealedSubclasses.forEach {
        include(shouldBeAbleToSendMessageToTaskExecutorTopic(TestFactory.random(it)))
    }
})

private fun shouldBeAbleToSendMessageToWorkflowEngineCommandsTopic(msg: WorkflowEngineMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to WorkflowEngineCommands topic" {
        // given
        val context = context()
        val builder = mockk<TypedMessageBuilder<WorkflowEngineEnvelope>>()
        val slotSchema = slot<AvroSchema<WorkflowEngineEnvelope>>()
        every { context.newOutputMessage(any(), capture(slotSchema)) } returns builder
        every { context.tenant } returns "tenant"
        every { context.namespace } returns "namespace"
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.sendAsync() } returns CompletableFuture.completedFuture(mockk())
        // when
        PulsarOutput.from(context).sendToWorkflowEngine(TopicType.COMMANDS)(msg, MillisDuration(0))
        // then
        verify {
            context.newOutputMessage(
                "persistent://tenant/namespace/workflow-engine-commands: ${msg.workflowName}",
                slotSchema.captured
            )
        }
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<WorkflowEngineEnvelope>()).avroSchema
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
        every { context.tenant } returns "tenant"
        every { context.namespace } returns "namespace"
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.sendAsync() } returns CompletableFuture.completedFuture(mockk())
        // when
        PulsarOutput.from(context).sendToTaskEngine(TopicType.COMMANDS)(msg, MillisDuration(0))
        // then
        verify {
            context.newOutputMessage(
                "persistent://tenant/namespace/task-engine-commands: ${msg.taskName}",
                slotSchema.captured
            )
        }
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<TaskEngineEnvelope>()).avroSchema
        verifyAll {
            builder.value(TaskEngineEnvelope.from(msg))
            builder.key("${msg.taskId}")
            builder.sendAsync()
        }
        confirmVerified(builder)
    }
}

private fun shouldBeAbleToSendMessageToMetricsPerNameTopic(msg: MetricsPerNameMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to MetricsPerName topic " {
        // given
        val context = context()
        val builder = mockk<TypedMessageBuilder<MetricsPerNameEnvelope>>()
        val slotSchema = slot<AvroSchema<MetricsPerNameEnvelope>>()
        every { context.newOutputMessage(any(), capture(slotSchema)) } returns builder
        every { context.tenant } returns "tenant"
        every { context.namespace } returns "namespace"
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.sendAsync() } returns CompletableFuture.completedFuture(mockk())
        // when
        PulsarOutput.from(context).sendToMetricsPerName()(msg)
        // then
        verify {
            context.newOutputMessage(
                "persistent://tenant/namespace/task-metrics: ${msg.taskName}",
                slotSchema.captured
            )
        }
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<MetricsPerNameEnvelope>()).avroSchema
        verifyAll {
            builder.value(MetricsPerNameEnvelope.from(msg))
            builder.key("${msg.taskName}")
            builder.sendAsync()
        }
        confirmVerified(builder)
    }
}

private fun shouldBeAbleToSendMessageToMetricsGlobalTopic(msg: MetricsGlobalMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to MetricsGlobal topic " {
        // given
        val context = context()
        val builder = mockk<TypedMessageBuilder<MetricsGlobalEnvelope>>()
        val slotSchema = slot<AvroSchema<MetricsGlobalEnvelope>>()
        val slotTopic = slot<String>()
        every { context.newOutputMessage(capture(slotTopic), capture(slotSchema)) } returns builder
        every { context.tenant } returns "tenant"
        every { context.namespace } returns "namespace"
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.sendAsync() } returns CompletableFuture.completedFuture(mockk())
        // when
        PulsarOutput.from(context).sendToMetricsGlobal()(msg)
        // then
        verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
        slotTopic.captured shouldBe "persistent://tenant/namespace/global-metrics"
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<MetricsGlobalEnvelope>()).avroSchema
        verify(exactly = 1) { builder.value(MetricsGlobalEnvelope.from(msg)) }
        verify(exactly = 1) { builder.sendAsync() }
        confirmVerified(builder)
    }
}

private fun shouldBeAbleToSendMessageToTaskExecutorTopic(msg: TaskExecutorMessage) = stringSpec {
    "${msg::class.simpleName!!} can be send to Worker topic" {
        val context = context()
        val builder = mockk<TypedMessageBuilder<TaskExecutorEnvelope>>()
        val slotSchema = slot<AvroSchema<TaskExecutorEnvelope>>()
        val slotTopic = slot<String>()
        every { context.newOutputMessage(capture(slotTopic), capture(slotSchema)) } returns builder
        every { context.tenant } returns "tenant"
        every { context.namespace } returns "namespace"
        every { builder.value(any()) } returns builder
        every { builder.key(any()) } returns builder
        every { builder.sendAsync() } returns CompletableFuture.completedFuture(mockk())
        // when
        PulsarOutput.from(context).sendToTaskExecutors()(msg)
        // then
        verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
        slotTopic.captured shouldBe "persistent://tenant/namespace/task-executor: ${msg.taskName}"
        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<TaskExecutorEnvelope>()).avroSchema
        verify(exactly = 1) { builder.value(TaskExecutorEnvelope.from(msg)) }
        verify(exactly = 1) { builder.key("${msg.taskId}") }
        verify(exactly = 1) { builder.sendAsync() }
        confirmVerified(builder)
    }
}

fun context(): Context {
    val context = mockk<Context>()
    every { context.logger } returns mockk(relaxed = true)

    return context
}
