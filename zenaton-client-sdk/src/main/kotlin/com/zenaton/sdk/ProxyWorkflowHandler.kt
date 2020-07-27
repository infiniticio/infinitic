package com.zenaton.sdk

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class ProxyWorkflowHandler : InvocationHandler {
    lateinit var dispatcher: Dispatcher

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {

        val parameterTypes = method.parameterTypes.map { it.name }

        println("method: $method")
        parameterTypes.map { println(convertPrimitive(it)) }

        val input = args?.mapIndexed { index: Int, param: Any? -> if (param == null) parameterTypes[index] else param }
        input?.map { println(it) }

        return "toto"
    }

    private fun convertPrimitive(klass: String) = when (klass) {
        "bytes" -> Byte::class.java
        "short" -> Short::class.java
        "int" -> Integer::class.java
        "long" -> Long::class.java
        "float" -> Float::class.java
        "double" -> Double::class.java
        "boolean" -> Boolean::class.java
        "char" -> Character::class.java
        else -> klass
    }
}
