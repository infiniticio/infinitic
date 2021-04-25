/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

@file:Suppress("unused")

package io.infinitic.tasks.executor

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.messages.TaskAttemptCompleted
import io.infinitic.common.tasks.engine.messages.TaskAttemptFailed
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.messages.ExecuteTaskAttempt
import io.infinitic.exceptions.tasks.ClassNotFoundException
import io.infinitic.exceptions.tasks.NoMethodFoundWithParameterCountException
import io.infinitic.exceptions.tasks.NoMethodFoundWithParameterTypesException
import io.infinitic.exceptions.tasks.ProcessingTimeoutException
import io.infinitic.exceptions.tasks.TooManyMethodsFoundWithParameterCountException
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.infinitic.tasks.executor.samples.SampleTaskWithBuggyRetry
import io.infinitic.tasks.executor.samples.SampleTaskWithContext
import io.infinitic.tasks.executor.samples.SampleTaskWithRetry
import io.infinitic.tasks.executor.samples.SampleTaskWithTimeout
import io.infinitic.tasks.executor.samples.TestingSampleTask
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope

fun mockSendToTaskEngine(slots: MutableList<TaskEngineMessage>): SendToTaskEngine {
    val sendToTaskEngine = mockk<SendToTaskEngine>()
    coEvery { sendToTaskEngine(capture(slots)) } just Runs

    return sendToTaskEngine
}

class TaskExecutorTests : StringSpec({
    val slots = mutableListOf<TaskEngineMessage>()
    val taskExecutorRegister = TaskExecutorRegisterImpl()
    val taskExecutor = TaskExecutor(mockSendToTaskEngine(slots), taskExecutorRegister)

    // ensure slots are emptied between each test
    beforeTest {
        slots.clear()
    }

    "Should be able to run an explicit method with 2 parameters" {
        val input = arrayOf(3, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getExecuteTaskAttempt("foo", "other", input, types)
        // when
        taskExecutor.registerTask("foo") { TestingSampleTask() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 1
        slots[0] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskName = msg.taskName,
            taskAttemptId = msg.taskAttemptId,
            taskRetrySequence = msg.taskRetrySequence,
            taskRetryIndex = msg.taskRetryIndex,
            taskReturnValue = MethodReturnValue.from("9"),
            taskMeta = msg.taskMeta
        )
    }

    "Should be able to run an explicit method with 2 parameters without parameterTypes" {
        val input = arrayOf(4, "3")
        // with
        val msg = getExecuteTaskAttempt("foo", "other", input, null)
        // when
        taskExecutor.registerTask("foo") { TestingSampleTask() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 1
        slots[0] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskName = msg.taskName,
            taskAttemptId = msg.taskAttemptId,
            taskRetrySequence = msg.taskRetrySequence,
            taskRetryIndex = msg.taskRetryIndex,
            taskReturnValue = MethodReturnValue.from("12"),
            taskMeta = msg.taskMeta
        )
    }

    "Should throw ClassNotFoundDuringTaskInstantiation when trying to process an unknown task" {
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getExecuteTaskAttempt("foo", "unknown", input, types)
        // when
        taskExecutor.unregisterTask("foo")
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 1
        slots[0].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[0] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskRetrySequence shouldBe msg.taskRetrySequence
        fail.taskRetryIndex shouldBe msg.taskRetryIndex
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.name shouldBe ClassNotFoundException::class.java.name
    }

    "Should throw NoMethodFoundWithParameterTypes when trying to process an unknown method" {
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getExecuteTaskAttempt("foo", "unknown", input, types)
        // when
        taskExecutor.registerTask("foo") { TestingSampleTask() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 1
        slots[0].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[0] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskRetrySequence shouldBe msg.taskRetrySequence
        fail.taskRetryIndex shouldBe msg.taskRetryIndex
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.name shouldBe NoMethodFoundWithParameterTypesException::class.java.name
    }

    "Should throw NoMethodFoundWithParameterCount when trying to process an unknown method without parameterTypes" {
        val input = arrayOf(2, "3")
        // with
        val msg = getExecuteTaskAttempt("foo", "unknown", input, null)
        // when
        taskExecutor.registerTask("foo") { TestingSampleTask() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 1
        slots[0].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[0] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskRetrySequence shouldBe msg.taskRetrySequence
        fail.taskRetryIndex shouldBe msg.taskRetryIndex
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.name shouldBe NoMethodFoundWithParameterCountException::class.java.name
    }

    "Should throw TooManyMethodsFoundWithParameterCount when trying to process an unknown method without parameterTypes" {
        val input = arrayOf(2, "3")
        // with
        val msg = getExecuteTaskAttempt("foo", "handle", input, null)
        // when
        taskExecutor.registerTask("foo") { TestingSampleTask() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 1
        slots[0].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[0] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskRetrySequence shouldBe msg.taskRetrySequence
        fail.taskRetryIndex shouldBe msg.taskRetryIndex
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.name shouldBe TooManyMethodsFoundWithParameterCountException::class.java.name
    }

    "Should retry with correct exception" {
        val input = arrayOf(2, "3")
        // with
        val msg = getExecuteTaskAttempt("foo", "handle", input, null)
        // when
        taskExecutor.registerTask("foo") { SampleTaskWithRetry() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 1
        slots[0].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[0] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskRetrySequence shouldBe msg.taskRetrySequence
        fail.taskRetryIndex shouldBe msg.taskRetryIndex
        fail.taskAttemptDelayBeforeRetry shouldBe MillisDuration(3000)
        fail.taskAttemptError.name shouldBe IllegalStateException::class.java.name
    }

    "Should throw when getRetryDelay throw an exception" {
        val input = arrayOf(2, "3")
        // with
        val msg = getExecuteTaskAttempt("foo", "handle", input, null)
        // when
        taskExecutor.registerTask("foo") { SampleTaskWithBuggyRetry() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 1
        slots[0].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[0] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskRetrySequence shouldBe msg.taskRetrySequence
        fail.taskRetryIndex shouldBe msg.taskRetryIndex
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.name shouldBe IllegalArgumentException::class.java.name
    }

    "Should be able to access context from task" {
        val input = arrayOf(2, "3")
        // with
        val msg = getExecuteTaskAttempt("foo", "handle", input, null)
        // when
        taskExecutor.registerTask("foo") { SampleTaskWithContext() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 1
        slots[0] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskName = msg.taskName,
            taskAttemptId = msg.taskAttemptId,
            taskRetrySequence = msg.taskRetrySequence,
            taskRetryIndex = msg.taskRetryIndex,
            taskReturnValue = MethodReturnValue.from("72"),
            taskMeta = msg.taskMeta
        )
    }

    "Should throw ProcessingTimeout if processing time is too long" {
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getExecuteTaskAttempt("foo", "handle", input, types)
        // when
        taskExecutor.registerTask("foo") { SampleTaskWithTimeout() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 1
        slots[0].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[0] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskRetrySequence shouldBe msg.taskRetrySequence
        fail.taskRetryIndex shouldBe msg.taskRetryIndex
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.name shouldBe ProcessingTimeoutException::class.java.name
    }
})

private fun getExecuteTaskAttempt(name: String, method: String, input: Array<out Any?>, types: List<String>?) = ExecuteTaskAttempt(
    taskName = TaskName(name),
    taskId = TaskId(),
    workflowId = null,
    workflowName = null,
    taskAttemptId = TaskAttemptId(),
    taskRetrySequence = TaskRetrySequence(12),
    taskRetryIndex = TaskRetryIndex(7),
    lastTaskError = null,
    methodName = MethodName(method),
    methodParameterTypes = types?.let { MethodParameterTypes(it) },
    methodParameters = MethodParameters.from(*input),
    taskOptions = TaskOptions(runningTimeout = .2F),
    taskMeta = TaskMeta()
)
