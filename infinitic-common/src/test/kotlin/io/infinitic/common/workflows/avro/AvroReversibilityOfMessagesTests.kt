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

package io.infinitic.common.workflows.avro

import io.infinitic.common.workflows.messages.ForWorkflowEngineMessage
import io.infinitic.avro.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import io.infinitic.common.fixtures.TestFactory
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecordBase
import kotlin.reflect.KClass

class AvroConsistencyTests : StringSpec({
    ForWorkflowEngineMessage::class.sealedSubclasses.forEach {
        val msg = TestFactory.random(it)
        include(messagesForWorkflowEngineShouldBeAvroReversible(msg))
    }

    checkAllSubTypesFromEnvelope<ForWorkflowEngineMessage>(this, AvroEnvelopeForWorkflowEngine())
})

fun messagesForWorkflowEngineShouldBeAvroReversible(msg: ForWorkflowEngineMessage) = stringSpec {
    "${msg::class.simpleName!!} should be avro-convertible to ${AvroEnvelopeForWorkflowEngine::class.simpleName}" {
        shouldNotThrowAny {
            val avroMsg = AvroConverter.toWorkflowEngine(msg)
            val msg2 = AvroConverter.fromWorkflowEngine(avroMsg)
            val avroMsg2 = AvroConverter.toWorkflowEngine(msg2)
            msg shouldBe msg2
            avroMsg shouldBe avroMsg2
        }
    }
}

inline fun <reified T> checkAllSubTypesFromEnvelope(config: StringSpec, msg: GenericRecord) {
    msg.schema.getField("type").schema().enumSymbols.forEach {
        val schema = msg.schema.getField(it).schema()
        config.include(checkEnvelopeSchema(it, schema, msg::class))
        val name = schema.types[1].name
        val namespace = schema.types[1].namespace
        config.include(checkConversionFromAvro<T>(name, namespace))
    }
}

fun checkEnvelopeSchema(field: String, schema: Schema, klass: KClass<out GenericRecord>) = stringSpec {
    "Checking schema for field $field of ${klass.simpleName}" {
        // check that type is an union
        schema.isUnion shouldBe true
        // check that first type is null
        schema.types[0].isNullable shouldBe true
        // check size
        schema.types.size shouldBe 2
    }
}

inline fun <reified T> checkConversionFromAvro(name: String, namespace: String) = stringSpec {
    "$name should be convertible from avro" {
        // get class name
        @Suppress("UNCHECKED_CAST")
        val klass = Class.forName("$namespace.$name").kotlin as KClass<SpecificRecordBase>
        val message = AvroConverter.fromAvroMessage(TestFactory.random(klass))
        (message is T) shouldBe true
    }
}
