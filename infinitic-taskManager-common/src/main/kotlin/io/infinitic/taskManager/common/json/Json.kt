package io.infinitic.common.json

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.infinitic.taskManager.common.exceptions.UserException
import org.apache.avro.specific.SpecificRecordBase

object Json {
    val mapper = jacksonObjectMapper()
        .addMixIn(SpecificRecordBase::class.java, AvroMixIn::class.java)
        .addMixIn(UserException::class.java, UserExceptionMixIn::class.java)

    // https://stackoverflow.com/questions/56742226/avro-generated-class-issue-with-json-conversion-kotlin
    abstract class AvroMixIn {
        @JsonIgnore
        abstract fun getSchema(): org.apache.avro.Schema
        @JsonIgnore
        abstract fun getSpecificData(): org.apache.avro.specific.SpecificData
    }

    abstract class UserExceptionMixIn {
        @JsonIgnore
        abstract fun getMessage(): String
    }

    fun stringify(msg: Any, pretty: Boolean = false): String = if (pretty) {
        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(msg)
    } else {
        mapper.writeValueAsString(msg)
    }

    fun <T : Any> parse(json: String, klass: Class<out T>): T = mapper.readValue(json, klass)

    inline fun <reified M : Any> parse(json: String) = parse(json, M::class.java)
}
