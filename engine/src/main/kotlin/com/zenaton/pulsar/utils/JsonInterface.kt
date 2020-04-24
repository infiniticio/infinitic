package com.zenaton.pulsar.utils

import kotlin.reflect.KClass

interface JsonInterface {
    fun stringify(msg: Any, pretty: Boolean = false): String

    fun <M : Any> parse(json: String, klass: KClass<M>): Any
}
