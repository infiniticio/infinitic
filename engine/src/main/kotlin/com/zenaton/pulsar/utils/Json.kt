package com.zenaton.pulsar.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

object Json : JsonInterface {
    val mapper = jacksonObjectMapper()

    override fun to(msg: Any): String {
        return mapper.writeValueAsString(msg)
    }

    override fun <M: Any> from(json: String, klass: KClass<M>) : Any {
        return mapper.readValue(json, klass.java)
    }
}