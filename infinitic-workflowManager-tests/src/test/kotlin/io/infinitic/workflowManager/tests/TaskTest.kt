package io.infinitic.workflowManager.tests

interface TaskTest {
    fun concat(str1: String, str2:String): String
}

class TaskTestImpl: TaskTest {
    override fun concat(str1: String, str2:String) = str1 + str2
}
