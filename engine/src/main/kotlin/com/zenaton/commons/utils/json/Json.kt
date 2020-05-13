package com.zenaton.commons.utils.json

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass
import org.apache.avro.specific.SpecificRecordBase

object Json : JsonInterface {
    private val mapper = jacksonObjectMapper().addMixIn(SpecificRecordBase::class.java, AvroMixIn::class.java)

    // https://stackoverflow.com/questions/56742226/avro-generated-class-issue-with-json-conversion-kotlin
    abstract class AvroMixIn {
        @JsonIgnore
        abstract fun getSchema(): org.apache.avro.Schema
        @JsonIgnore
        abstract fun getSpecificData(): org.apache.avro.specific.SpecificData
    }

    override fun stringify(msg: Any, pretty: Boolean): String {
        return if (pretty) {
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(msg)
        } else {
            mapper.writeValueAsString(msg)
        }
    }

    override fun <M : Any> parse(json: String, klass: KClass<M>): M {
        return mapper.readValue(json, klass.java)
    }
}
