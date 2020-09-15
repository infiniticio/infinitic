package io.infinitic.workflowManager.common

import io.infinitic.common.json.Json
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.workflowManager.common.data.commands.DispatchTask

fun main() {

    var t = DispatchTask(
        taskName = TaskName("er"),
        taskInput = TaskInput("r")
    )

    println(t)
    println(Json.stringify(t))
    println(t.hash())
}
