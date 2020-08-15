package com.zenaton.jobManager.worker

fun main() {
    println(TestWithSuspend::class.java.methods.filter { it.name == "handle" }.first())
}
