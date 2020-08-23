package io.infinitic.taskManager.common

import io.infinitic.common.json.Json
import io.infinitic.taskManager.common.data.TaskAttemptError
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskOutput
import io.infinitic.taskManager.common.messages.CancelTask
import io.infinitic.taskManager.common.messages.RunTask
import io.infinitic.taskManager.common.messages.TaskAttemptFailed
import io.infinitic.taskManager.common.utils.TestFactory
import io.infinitic.taskManager.messages.AvroCancelTask
import io.infinitic.taskManager.messages.AvroRunTask

fun main() {
    val e = TestFactory.random(TaskAttemptFailed::class)
    println(e)
    val s = Json.stringify(e)
    println(s)
    val d = Json.parse<TaskAttemptFailed>(s)
    println(d)
    println(d == e)
}
