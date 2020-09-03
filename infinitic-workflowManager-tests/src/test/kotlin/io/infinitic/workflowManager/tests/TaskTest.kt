package io.infinitic.workflowManager.tests

interface TaskTest {
    fun log()
}

class TaskTestImpl {
    companion object {
        var log = ""
    }

    fun log() {
        log += "1"
    }
}
