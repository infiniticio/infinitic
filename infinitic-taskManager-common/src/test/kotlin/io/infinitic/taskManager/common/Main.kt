package io.infinitic.taskManager.common

import io.infinitic.taskManager.common.data.TaskInput

fun main() {
    println(TaskInput(1, 2) == TaskInput(1, 2))
//    val e = TestFactory.random(TaskInput::class)
//    println(e)
//    val s = Json.stringify(e)
//    println(s)
//    val d = Json.parse<TaskInput>(s)
//    println(d)
//    println(d == e)
}
