package com.zenaton.jobManager.avro

import com.zenaton.jobManager.messages.Message
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.jobManager.messages.envelopes.ForJobEngineMessage
import com.zenaton.jobManager.messages.envelopes.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.envelopes.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.envelopes.ForWorkerMessage
import com.zenaton.jobManager.messages.envelopes.ForWorkflowEngineMessage
import com.zenaton.jobManager.utils.TestFactory
import com.zenaton.workflowManager.messages.AvroTaskCompleted
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import org.apache.avro.specific.SpecificRecordBase

class AvroConsistencyTests : StringSpec({

    include(
        `Avro messages in AvroFor*Message should implement For*Message after conversion`<ForJobEngineMessage>(
            AvroEnvelopeForJobEngine()
        )
    )

    include(
        `Avro messages in AvroFor*Message should implement For*Message after conversion`<ForMonitoringGlobalMessage>(
            AvroEnvelopeForMonitoringGlobal()
        )
    )
    include(
        `Avro messages in AvroFor*Message should implement For*Message after conversion`<ForMonitoringPerNameMessage>(
            AvroEnvelopeForMonitoringPerName()
        )
    )
    include(
        `Avro messages in AvroFor*Message should implement For*Message after conversion`<ForWorkerMessage>(
            AvroEnvelopeForWorker()
        )
    )

    "Messages used from AvroForWorkflowsMessage should implement ForWorkflowsMessage after conversion" {
        val taskCompleted = AvroConverter.convertFromAvro(TestFactory.get(AvroTaskCompleted::class))
        (taskCompleted is ForWorkflowEngineMessage) shouldBe true
        (taskCompleted is Message) shouldBe true
    }

    "message implementing a For*Message interface should be convertible to AvroFor*Message" {
        Message::class.sealedSubclasses.forEach {
            shouldNotThrowAny {
                val msg = TestFactory.get(it)
                if (msg is ForWorkerMessage) {
                    AvroConverter.toWorkers(msg)
                }
                if (msg is ForJobEngineMessage) {
                    AvroConverter.toJobEngine(msg)
                }
                if (msg is ForWorkflowEngineMessage) {
                    AvroConverter.toWorkflowEngine(msg)
                }
                if (msg is ForMonitoringPerNameMessage) {
                    AvroConverter.toMonitoringPerName(msg)
                }
                if (msg is ForMonitoringGlobalMessage) {
                    AvroConverter.toMonitoringGlobal(msg)
                }
            }
        }
    }
})

private inline fun <reified T> `Avro messages in AvroFor*Message should implement For*Message after conversion`(msg: SpecificRecordBase) = stringSpec {
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
