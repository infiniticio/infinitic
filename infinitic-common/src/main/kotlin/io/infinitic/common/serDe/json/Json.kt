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

package io.infinitic.common.json

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.avro.specific.SpecificRecordBase

object Json {
    val mapper = jacksonObjectMapper()
        .addMixIn(SpecificRecordBase::class.java, AvroMixIn::class.java)
        .addMixIn(Exception::class.java, ExceptionMixIn::class.java)
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule())

    // https://stackoverflow.com/questions/56742226/avro-generated-class-issue-with-json-conversion-kotlin
    abstract class AvroMixIn {
        @JsonIgnore
        abstract fun getSchema(): org.apache.avro.Schema
        @JsonIgnore
        abstract fun getSpecificData(): org.apache.avro.specific.SpecificData
    }

    abstract class ExceptionMixIn {
        @JsonIgnore abstract fun getMessage(): String
    }

    fun stringify(msg: Any?, pretty: Boolean = false): String = if (pretty) {
        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(msg)
    } else {
        mapper.writeValueAsString(msg)
    }

    fun <T : Any> parse(json: String, klass: Class<out T>): T = mapper.readValue(json, klass)
}
