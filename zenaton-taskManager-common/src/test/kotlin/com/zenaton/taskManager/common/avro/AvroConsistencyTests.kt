package com.zenaton.taskManager.common.avro

import com.zenaton.taskManager.common.messages.ForMonitoringGlobalMessage
import com.zenaton.taskManager.common.messages.ForMonitoringPerNameMessage
import com.zenaton.taskManager.common.messages.ForTaskEngineMessage
import com.zenaton.taskManager.common.messages.ForWorkerMessage
import com.zenaton.taskManager.common.utils.TestFactory
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForWorker
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.TestConfiguration
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import kotlin.reflect.KClass
import org.apache.avro.specific.SpecificRecordBase

class AvroConsistencyTests : StringSpec({
    ForTaskEngineMessage::class.sealedSubclasses.forEach {
        include(checkAvroConversionToEnvelopeForTaskEngine(TestFactory.random(it)))
    }

    ForMonitoringPerNameMessage::class.sealedSubclasses.forEach {
        include(checkAvroConversionToEnvelopeForMonitoringPerName(TestFactory.random(it)))
    }

    ForMonitoringGlobalMessage::class.sealedSubclasses.forEach {
        include(checkAvroConversionToEnvelopeForMonitoringGlobal(TestFactory.random(it)))
    }

    ForWorkerMessage::class.sealedSubclasses.forEach {
        include(checkAvroConversionToEnvelopeForWorker(TestFactory.random(it)))
    }

    // From Avro
    checkAllSubTypesFromEnvelope<ForWorkerMessage>(this, AvroEnvelopeForWorker())
    checkAllSubTypesFromEnvelope<ForTaskEngineMessage>(this, AvroEnvelopeForTaskEngine())
    checkAllSubTypesFromEnvelope<ForMonitoringGlobalMessage>(this, AvroEnvelopeForMonitoringGlobal())
    checkAllSubTypesFromEnvelope<ForMonitoringPerNameMessage>(this, AvroEnvelopeForMonitoringPerName())
})

internal fun checkAvroConversionToEnvelopeForTaskEngine(msg: ForTaskEngineMessage) = stringSpec {
    "${msg::class.simpleName!!} should be convertible to ${AvroEnvelopeForTaskEngine::class.simpleName}" {
        shouldNotThrowAny {
            AvroConverter.toTaskEngine(msg)
        }
    }
}

internal fun checkAvroConversionToEnvelopeForMonitoringPerName(msg: ForMonitoringPerNameMessage) = stringSpec {
    "${msg::class.simpleName!!} should be convertible to ${AvroEnvelopeForMonitoringPerName::class.simpleName}" {
        shouldNotThrowAny {
            AvroConverter.toMonitoringPerName(msg)
        }
    }
}

internal fun checkAvroConversionToEnvelopeForMonitoringGlobal(msg: ForMonitoringGlobalMessage) = stringSpec {
    "${msg::class.simpleName!!} should be convertible to ${AvroEnvelopeForMonitoringGlobal::class.simpleName}" {
        shouldNotThrowAny {
            AvroConverter.toMonitoringGlobal(msg)
        }
    }
}

internal fun checkAvroConversionToEnvelopeForWorker(msg: ForWorkerMessage) = stringSpec {
    "${msg::class.simpleName!!} should be convertible to ${AvroEnvelopeForWorker::class.simpleName}" {
        shouldNotThrowAny {
            AvroConverter.toWorkers(msg)
        }
    }
}

internal inline fun <reified T> checkAllSubTypesFromEnvelope(config: TestConfiguration, msg: GenericRecord) {
    msg.schema.getField("type").schema().enumSymbols.forEach {
        val schema = msg.schema.getField(it).schema()
        config.include(checkEnvelopeSchema(it, schema, msg::class))
        val name = schema.types[1].name
        val namespace = schema.types[1].namespace
        config.include(checkConversionFromAvro<T>(name, namespace))
    }
}

internal fun checkEnvelopeSchema(field: String, schema: Schema, klass: KClass<out GenericRecord>) = stringSpec {
    "Checking schema for field $field of ${klass.simpleName}" {
        // check that type is an union
        schema.isUnion shouldBe true
        // check that first type is null
        schema.types[0].isNullable shouldBe true
        // check size
        schema.types.size shouldBe 2
    }
}

internal inline fun <reified T> checkConversionFromAvro(name: String, namespace: String) = stringSpec {
    "$name should be convertible from avro" {
        // get class name
        @Suppress("UNCHECKED_CAST")
        val klass = Class.forName("$namespace.$name").kotlin as KClass<SpecificRecordBase>
        val message = AvroConverter.fromAvroMessage(TestFactory.random(klass))
        (message is T) shouldBe true
    }
}
