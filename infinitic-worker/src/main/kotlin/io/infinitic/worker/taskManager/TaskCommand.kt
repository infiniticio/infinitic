package io.infinitic.worker.taskManager

import io.infinitic.common.taskManager.data.TaskOptions
import java.lang.reflect.Method

internal data class TaskCommand(
    val job: Any,
    val method: Method,
    val input: Array<out Any?>,
    val options: TaskOptions
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TaskCommand

        if (job != other.job) return false
        if (method != other.method) return false
        if (!input.contentEquals(other.input)) return false
        if (options != other.options) return false

        return true
    }

    override fun hashCode(): Int {
        var result = job.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + input.contentHashCode()
        result = 31 * result + options.hashCode()
        return result
    }
}
