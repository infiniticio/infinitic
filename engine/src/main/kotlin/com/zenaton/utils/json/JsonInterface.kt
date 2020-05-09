package com.zenaton.utils.json

import kotlin.reflect.KClass

interface JsonInterface {
    fun stringify(msg: Any, pretty: Boolean = false): String

    fun <M : Any> parse(json: String, klass: KClass<M>): M
}
