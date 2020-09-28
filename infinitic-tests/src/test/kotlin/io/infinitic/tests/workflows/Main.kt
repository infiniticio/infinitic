package io.infinitic.tests.workflows

import io.infinitic.worker.workflowTask.Workflow

fun main() {
    test().mmm()
}

class test : Workflow() {

    fun mmm() {
//        val task = CommandProxyHandler(TaskA::class.java) { null }.instance()
//        println( task::class.java.methods.map { println(it) })
//        println( task::class.java.fields.map { println(it) })
//        val c = Class.forName(task.toString())
//        println(c)
//        println(c.interfaces.contains(Task::class.java))
//        println(c.interfaces.contains(Workflow::class.java))
    }
}
