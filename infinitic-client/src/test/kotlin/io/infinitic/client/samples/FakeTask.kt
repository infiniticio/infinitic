package io.infinitic.client.samples

import io.infinitic.common.taskManager.Task
import io.infinitic.common.taskManager.data.TaskId

internal interface FakeTask : Task {
    fun m1()
    fun m1(i: Int): String
    fun m1(str: String?): Any?
    fun m1(p1: Int, p2: String): String
    fun m1(id: FakeInterface): TaskId
    fun m2(): Boolean
}
