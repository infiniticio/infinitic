package io.infinitic.tests

import io.infinitic.common.workflows.data.properties.PropertiesNameHash
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName

fun main() {
    val p1 = PropertiesNameHash(mutableMapOf(PropertyName("1") to PropertyHash("a")))

//    val p2 = PropertiesHashValue(p1.toMutableMap())
    val p2 = p1.copy()

    println(p1)
    println(p2)

    p1[PropertyName("1")] = PropertyHash("b")

    println(p1)
    println(p2)
}
