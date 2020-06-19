package com.zenaton.workflowManager.avro

import com.zenaton.workflowManager.TestFactory
import com.zenaton.workflowManager.messages.AvroTaskCompleted
import com.zenaton.workflowManager.messages.Message
import com.zenaton.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import com.zenaton.workflowManager.messages.envelopes.ForDecisionEngineMessage
import com.zenaton.workflowManager.messages.envelopes.ForTaskEngineMessage
import com.zenaton.workflowManager.messages.envelopes.ForWorkflowEngineMessage
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import org.apache.avro.specific.SpecificRecordBase

class AvroConsistencyTests : StringSpec({

    include(
        `Avro messages in envelope AvroEnvelopeForWorkflowEngine should implement ForWorkflowEngineMessage after conversion`()
    )

    "Messages used from AvroForJobMessage should implement ForWorkflowsMessage after conversion" {
        val taskCompleted = AvroConverter.convertFromAvro(TestFactory.get(AvroTaskCompleted::class))
        (taskCompleted is ForWorkflowEngineMessage) shouldBe true
        (taskCompleted is Message) shouldBe true
    }

    "message implementing an For*Message interface should be convertible to AvroEnvelopeFor*" {
        Message::class.sealedSubclasses.forEach {
            shouldNotThrowAny {
                val msg = TestFactory.get(it)
                if (msg is ForWorkflowEngineMessage) {
                    AvroConverter.toWorkflowEngine(msg)
                }
                if (msg is ForDecisionEngineMessage) {
                    AvroConverter.toDecisionEngine(msg)
                }
                if (msg is ForTaskEngineMessage) {
                    AvroConverter.toTaskEngine(msg)
                }
            }
        }
    }
})

private fun `Avro messages in envelope AvroEnvelopeForWorkflowEngine should implement ForWorkflowEngineMessage after conversion`() =
    `Avro messages in envelope AvroFor*Message should implement For*Message after conversion`<ForWorkflowEngineMessage>(AvroEnvelopeForWorkflowEngine())


private inline fun <reified T> `Avro messages in envelope AvroFor*Message should implement For*Message after conversion`(msg: SpecificRecordBase) = stringSpec {
    msg.schema.getField("type").schema().enumSymbols.forEach {
        val schema = msg.schema.getField(it).schema()
        // check that type is an union
        schema.isUnion shouldBe true
        // check that first type is null
        schema.types[0].isNullable shouldBe true
        // check that, if known, it implements Message and For*Message after conversion
        try {
            // get class name
            @Suppress("UNCHECKED_CAST")
            val klass = Class.forName("${schema.types[1].namespace}.${schema.types[1].name}").kotlin as KClass<SpecificRecordBase>
            val message = AvroConverter.convertFromAvro(TestFactory.get(klass))
            (message is T) shouldBe true
            (message is Message) shouldBe true
        } catch (e: ClassNotFoundException) {}
    }
}

