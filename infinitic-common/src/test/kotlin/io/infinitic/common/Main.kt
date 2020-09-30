package io.infinitic.common

import io.infinitic.common.json.Json
import io.infinitic.common.tasks.data.MethodParameterTypes

fun main() {
    val m = MethodParameterTypes(listOf("a", "b"))

    println(Json.stringify(m))
    val m2 = Json.parse<MethodParameterTypes>(Json.stringify(m))
    println(m2)
}
