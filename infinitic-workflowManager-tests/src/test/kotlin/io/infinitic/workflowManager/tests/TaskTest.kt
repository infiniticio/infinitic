package io.infinitic.workflowManager.tests

interface TaskTest {
    fun concat(str1: String, str2: String): String
    fun reverse(str: String): String
}

class TaskTestImpl : TaskTest {
    override fun concat(str1: String, str2: String) = str1 + str2
    override fun reverse(str: String) = str.reversed()

}
