package io.infinitic.taskManager.common

import io.infinitic.common.json.Json
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskOutput
import io.infinitic.taskManager.common.messages.CancelTask
import io.infinitic.taskManager.common.messages.RunTask
import io.infinitic.taskManager.common.utils.TestFactory
import io.infinitic.taskManager.messages.AvroCancelTask
import io.infinitic.taskManager.messages.AvroRunTask

fun main() {
    println(TaskOutput(arrayOf(1)) == TaskOutput(arrayOf(1)))
}
