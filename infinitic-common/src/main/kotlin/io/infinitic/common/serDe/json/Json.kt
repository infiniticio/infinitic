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

package io.infinitic.common.serDe.json

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import org.apache.avro.specific.SpecificRecordBase
import java.io.IOException

object Json {
    private val mapper = jsonMapper {
        addMixIn(SpecificRecordBase::class.java, AvroMixIn::class.java)
        addMixIn(Exception::class.java, ExceptionMixIn::class.java)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        addModule(JavaTimeModule())
        addModule(KotlinModule.Builder().build())
    }

    fun stringify(msg: Any?, pretty: Boolean = false): String = when (pretty) {
        true -> mapper.writerWithDefaultPrettyPrinter().writeValueAsString(msg)
        false -> mapper.writeValueAsString(msg)
    }

    fun <T> parse(json: String, klass: Class<out T>): T = mapper.readValue(json, klass)

    /**
     * Cause should not be included to the json, as it triggers
     * a circular reference when cause = this
     */
    private abstract class ExceptionMixIn {
        @JsonIgnore abstract fun getCause(): Throwable
        @JsonIgnore abstract fun getMessage(): String
    }

    /**
     * Schema and Data should not be included to the json
     * https://stackoverflow.com/questions/56742226/avro-generated-class-issue-with-json-conversion-kotlin
     *
     * IMPORTANT: properties of generated Avro classes MUST have public visibility
     */
    private abstract class AvroMixIn {
        @JsonIgnore abstract fun getSchema(): org.apache.avro.Schema
        @JsonIgnore abstract fun getSpecificData(): org.apache.avro.specific.SpecificData

        @JsonSerialize(using = AvroListStringSerializer::class)
        abstract fun getListOfString(): List<String>
    }

    // https://issues.apache.org/jira/browse/AVRO-2702
    private class AvroListStringSerializer : JsonSerializer<List<String>>() {
        @Throws(IOException::class)
        override fun serialize(value: List<String>, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeStartArray()
            for (o in value) { gen.writeString(o) }
            gen.writeEndArray()
        }
    }
}
