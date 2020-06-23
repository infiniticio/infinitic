package com.zenaton.jobManager.pulsar.utils

import com.zenaton.common.json.Json
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import java.io.File
import kotlin.reflect.KClass
import org.apache.avro.specific.SpecificRecordBase
import org.apache.pulsar.client.impl.schema.AvroSchema

/**
 * This class creates files in /build/schemas, used to upload schemas to topics
 */
fun main() {
    class Schema(klass: KClass<out SpecificRecordBase>) {
        val type = "AVRO"
        val properties = mapOf(
            "__alwaysAllowNull" to "true",
            "__jsr310ConversionEnabled" to "false"
        )
        val schema = AvroSchema.of(klass.java).avroSchema.toString()
    }

    listOf(
        AvroEnvelopeForJobEngine::class,
        AvroEnvelopeForWorker::class,
        AvroEnvelopeForMonitoringPerName::class,
        AvroEnvelopeForMonitoringGlobal::class
    ).map { klass ->
        File(System.getProperty("user.dir") + "/build/schemas/${klass.simpleName}.schema")
            .also { it.parentFile.mkdirs() }
            .writeText(Json.stringify(Schema(klass)))
    }
}
