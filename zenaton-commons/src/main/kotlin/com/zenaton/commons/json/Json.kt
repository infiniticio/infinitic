package com.zenaton.commons.json

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.avro.specific.SpecificRecordBase

object Json {
    val mapper = jacksonObjectMapper().addMixIn(SpecificRecordBase::class.java, AvroMixIn::class.java)

    // https://stackoverflow.com/questions/56742226/avro-generated-class-issue-with-json-conversion-kotlin
    abstract class AvroMixIn {
        @JsonIgnore
        abstract fun getSchema(): org.apache.avro.Schema
        @JsonIgnore
        abstract fun getSpecificData(): org.apache.avro.specific.SpecificData
    }

    fun stringify(msg: Any, pretty: Boolean = false): String {
        return if (pretty) {
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(msg)
        } else {
            mapper.writeValueAsString(msg)
        }
    }

    inline fun <reified M : Any> parse(json: String): M {
        return mapper.readValue(json, M::class.java)
    }
}
