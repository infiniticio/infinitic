package com.zenaton.workflowManager.pulsar.dispatcher

import com.zenaton.workflowManager.avroConverter.AvroConverter
import com.zenaton.workflowManager.messages.ForWorkflowEngineMessage
import com.zenaton.workflowManager.messages.Message
import com.zenaton.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import com.zenaton.workflowManager.pulsar.utils.TestFactory
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifyAll
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.TypedMessageBuilder
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context
import org.slf4j.Logger

class PulsarAvroDispatcherTests : StringSpec({
    // From Message
    Message::class.sealedSubclasses.forEach {
        val msg = TestFactory.random(it)
        if (msg is ForWorkflowEngineMessage) {
            include(checkMessageCanBeDispatched(msg))
        }
    }
})

private fun checkMessageCanBeDispatched(msg: ForWorkflowEngineMessage) = stringSpec {
    "${msg::class.simpleName!!} can be dispatched" {
        shouldNotThrowAny {
            val avro = AvroConverter.toWorkflowEngine(msg)
            val context = mockk<Context>()
            every { context.logger } returns mockk<Logger>(relaxed = true)
            val builder = mockk<TypedMessageBuilder<AvroEnvelopeForWorkflowEngine>>()
            val slotSchema = slot<AvroSchema<AvroEnvelopeForWorkflowEngine>>()
            every { context.newOutputMessage<AvroEnvelopeForWorkflowEngine>(any(), capture(slotSchema)) } returns builder
            every { builder.value(any()) } returns builder
            every { builder.key(any()) } returns builder
            every { builder.send() } returns mockk<MessageId>()
            // when
            PulsarAvroDispatcher(context).toWorkflowEngine(avro)
            // then
            verifyAll {
                context.newOutputMessage(Topic.WORKFLOW_ENGINE.get(), slotSchema.captured)
                context.logger
            }
            slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroEnvelopeForWorkflowEngine::class.java).avroSchema
            confirmVerified(context)
            verifyAll {
                builder.value(avro)
                builder.key(avro.workflowId)
                builder.send()
            }
            confirmVerified(builder)
        }
    }
}
