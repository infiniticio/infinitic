package io.infinitic.common

import io.infinitic.common.workflows.data.workflows.WorkflowId

fun main() {
    val id = WorkflowId()
    val id2 = id.copy()

    println(id == id2)
}
