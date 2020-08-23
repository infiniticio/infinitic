package io.infinitic.taskManager.common

import io.infinitic.common.json.Json
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.messages.TaskAttemptFailed
import io.infinitic.taskManager.common.utils.TestFactory

fun main() {
    val e = TestFactory.random(TaskInput::class)
    println(e)
    val s = Json.stringify(e)
    println(s)
    val d = Json.parse<TaskInput>(s)
    println(d)
    println(d == e)
}
