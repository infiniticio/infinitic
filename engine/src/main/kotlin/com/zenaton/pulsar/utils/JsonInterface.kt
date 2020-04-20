package com.zenaton.pulsar.utils

import kotlin.reflect.KClass

interface JsonInterface {
    fun to(msg: Any): String

    fun <M : Any> from(json: String, klass: KClass<M>): Any
}
