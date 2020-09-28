package io.infinitic.tests.workflows.samples

import io.infinitic.common.taskManager.Task

interface TaskA : Task {
    fun concat(str1: String, str2: String): String
    fun reverse(str: String): String
}

class TaskAImpl : TaskA {
    override fun concat(str1: String, str2: String) = str1 + str2
    override fun reverse(str: String) = str.reversed()
}
