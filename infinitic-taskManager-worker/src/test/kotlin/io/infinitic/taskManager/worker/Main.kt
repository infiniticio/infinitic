package io.infinitic.taskManager.worker

import io.infinitic.taskManager.common.data.TaskAttemptContext
import io.infinitic.taskManager.common.data.TaskAttemptId
import io.infinitic.taskManager.common.data.TaskAttemptIndex
import io.infinitic.taskManager.common.data.TaskAttemptRetry
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.data.TaskOptions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

fun main() {
    val t= TestWithContext()
    println(t::class.java.name)
     val p = TestWithContext::class.memberProperties.find {
         it.returnType.javaType.typeName == TaskAttemptContext::class.java.name
     }
    p?.javaField?.set(t, TaskAttemptContext(
        taskId = TaskId(),
        taskAttemptId = TaskAttemptId(),
        taskAttemptIndex = TaskAttemptIndex(23),
        taskAttemptRetry = TaskAttemptRetry(2),
        taskMeta = TaskMeta(),
        taskOptions = TaskOptions()
    ))
    println(t.context)
}
