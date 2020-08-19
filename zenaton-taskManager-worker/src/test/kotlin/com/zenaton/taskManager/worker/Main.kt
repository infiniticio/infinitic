package com.zenaton.taskManager.worker

fun main() {
    val m = TestWithRetry::class.java.methods.find { it.name == "getRetryDelay" }

    println(Float::class.javaObjectType.name)
    println(m!!.returnType.typeName)
}
