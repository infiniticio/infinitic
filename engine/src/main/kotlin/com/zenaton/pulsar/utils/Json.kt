package com.zenaton.pulsar.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

object Json : JsonInterface {
    val mapper = jacksonObjectMapper()

    override fun stringify(msg: Any, pretty: Boolean): String {
        return if (pretty) {
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(msg)
        } else {
            mapper.writeValueAsString(msg)
        }
    }

    override fun <M : Any> parse(json: String, klass: KClass<M>): Any {
        return mapper.readValue(json, klass.java)
    }
}
