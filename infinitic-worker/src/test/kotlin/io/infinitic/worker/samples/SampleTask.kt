package io.infinitic.worker.samples

import io.infinitic.common.tasks.Task

interface SampleTask : Task {
    fun handle(i: Int, j: String): String
    fun handle(i: Int, j: Int): String
    fun other(i: Int, j: String): String
}

class TestingSampleTask() : SampleTask {
    override fun handle(i: Int, j: String) = (i * j.toInt()).toString()
    override fun handle(i: Int, j: Int) = (i * j).toString()
    override fun other(i: Int, j: String) = (i * j.toInt()).toString()
}
