package com.zenaton.jobManager.pulsar.avro

import com.zenaton.jobManager.messages.AvroForEngineMessage
import com.zenaton.jobManager.messages.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.AvroForMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.AvroForWorkerMessage
import com.zenaton.jobManager.messages.Message
import com.zenaton.jobManager.messages.interfaces.ForEngineMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.interfaces.ForWorkerMessage
import com.zenaton.jobManager.utils.TestFactory
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import org.apache.avro.specific.SpecificRecordBase

class AvroConsistencyTests : StringSpec({

    include(`Avro messages in AvroFor*Message should implement For*Message after conversion`<ForEngineMessage>(AvroForEngineMessage()))
    include(`Avro messages in AvroFor*Message should implement For*Message after conversion`<ForMonitoringGlobalMessage>(AvroForMonitoringGlobalMessage()))
    include(`Avro messages in AvroFor*Message should implement For*Message after conversion`<ForMonitoringPerNameMessage>(AvroForMonitoringPerNameMessage()))
    include(`Avro messages in AvroFor*Message should implement For*Message after conversion`<ForMonitoringPerInstanceMessage>(AvroForMonitoringPerInstanceMessage()))
    include(`Avro messages in AvroFor*Message should implement For*Message after conversion`<ForWorkerMessage>(AvroForWorkerMessage()))

    "message implementing a For*Message interface should be convertible to AvroFor*Message" {
        Message::class.sealedSubclasses.forEach {
            shouldNotThrowAny {
                val msg = TestFactory.get(it)
                if (msg is ForWorkerMessage) {
                    AvroConverter.toAvroForWorkerMessage(msg)
                }
                if (msg is ForEngineMessage) {
                    AvroConverter.toAvroForEngineMessage(msg)
                }
                if (msg is ForMonitoringPerInstanceMessage) {
                    AvroConverter.toAvroForMonitoringPerInstanceMessage(msg)
                }
                if (msg is ForMonitoringPerNameMessage) {
                    AvroConverter.toAvroForMonitoringPerNameMessage(msg)
                }
                if (msg is ForMonitoringGlobalMessage) {
                    AvroConverter.toAvroForMonitoringGlobalMessage(msg)
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
        // get class name
        val klass = Class.forName("${schema.types[1].namespace}.${schema.types[1].name}").kotlin as KClass<SpecificRecordBase>
        // check that it implements Message and For*Message after conversion
        val msg = AvroConverter.convertFromAvro(TestFactory.get(klass))
        (msg is T) shouldBe true
        (msg is Message) shouldBe true
    }
}
