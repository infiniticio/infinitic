package io.infinitic.common.workflows

import com.sksamuel.avro4k.Avro
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.workflows.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.messages.WorkflowEngineMessage
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class EnvelopeTests : StringSpec({

    WorkflowEngineMessage::class.sealedSubclasses.map {
        val msg = TestFactory.random(it)

        "WorkflowEngineEnvelope(${msg::class.simpleName}) should be avro-convertible" {
            shouldNotThrowAny {
                val envelope = WorkflowEngineEnvelope.from(msg)
                val ser = WorkflowEngineEnvelope.serializer()
                val byteArray = Avro.default.encodeToByteArray(ser, envelope)
                val envelope2 = Avro.default.decodeFromByteArray(ser, byteArray)
                envelope shouldBe envelope2
            }
        }
    }
})
