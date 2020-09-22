package io.infinitic.workflowManager.engine.utils

fun main() {
    val r = listOf(1, 2, 3, 4, 5).map {
        println(it)

        it> 9
    }.any { it }

    println(r)
}
