package io.infinitic.common

import io.infinitic.common.json.Json
import io.infinitic.common.serDe.SerializedData

sealed class Obj
data class Obj1(val p: String) : Obj()
data class Obj2(val p: String) : Obj()
data class Obj3(val p: String) : Obj()

fun main() {
    val j1 = Json.stringify(Obj1("1"))
    println(j1)
    println(Json.parse(j1, Obj1::class.java))

    val l = listOf(Obj1("1"), Obj2("2"))
    println(l)
    val s = SerializedData.from(l)
    val d = s.deserialize()
    if (d is Collection<*>) d.map { println(it!!::class.java) }

    val ss = Json.stringify(l)
    println(ss)
    println(Json.parse<List<Obj>>(ss, l::class.java))
}
