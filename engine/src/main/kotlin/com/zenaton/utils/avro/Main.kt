package com.zenaton.utils.avro

import com.zenaton.messages.taskAttempts.AvroTaskAttemptMessage
import com.zenaton.messages.tasks.AvroTaskMessage
import com.zenaton.utils.json.Json
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
        AvroTaskMessage::class,
        AvroTaskAttemptMessage::class
    ).map {
        val file = File(System.getProperty("user.dir") + "/build/schemas/${it.simpleName}.schema")
        file.also { it.parentFile.mkdirs() }
            .writeText(Json.stringify(Schema(it)))
    }
}
