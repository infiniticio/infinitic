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

import io.infinitic.common.data.methods.MethodInput
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodOutput
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskAttemptRetry
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskRetry
import io.infinitic.common.tasks.engine.messages.TaskAttemptCompleted
import io.infinitic.common.tasks.engine.messages.TaskAttemptFailed
import io.infinitic.common.tasks.engine.messages.TaskAttemptStarted
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.tasks.exceptions.ClassNotFoundDuringInstantiation
import io.infinitic.common.tasks.exceptions.NoMethodFoundWithParameterCount
import io.infinitic.common.tasks.exceptions.NoMethodFoundWithParameterTypes
import io.infinitic.common.tasks.exceptions.ProcessingTimeout
import io.infinitic.common.tasks.exceptions.RetryDelayHasWrongReturnType
import io.infinitic.common.tasks.exceptions.TooManyMethodsFoundWithParameterCount
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.tasks.executor.samples.SampleTaskWithBadTypeRetry
import io.infinitic.tasks.executor.samples.SampleTaskWithBuggyRetry
import io.infinitic.tasks.executor.samples.SampleTaskWithContext
import io.infinitic.tasks.executor.samples.SampleTaskWithRetry
import io.infinitic.tasks.executor.samples.SampleTaskWithTimeout
import io.infinitic.tasks.executor.samples.TestingSampleTask
import io.infinitic.tasks.executor.transport.TaskExecutorOutput
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope

class MockTaskExecutorOutput(slots: MutableList<TaskEngineMessage>) : TaskExecutorOutput {
    override val sendToTaskEngineFn = mockk<SendToTaskEngine>()

    init {
        coEvery { sendToTaskEngineFn(capture(slots), any()) } just Runs
    }
}

class TaskExecutorTests : StringSpec({
    val slots = mutableListOf<TaskEngineMessage>()
    val taskExecutor = TaskExecutor(MockTaskExecutorOutput(slots))

    // ensure slots are emptied between each test
    beforeTest {
        slots.clear()
    }

    "Should be able to run an explicit method with 2 parameters" {
        val input = arrayOf(3, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getTaskExecutorMessage("foo", "other", input, types)
        // when
        taskExecutor.register("foo") { TestingSampleTask() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskRetry = msg.taskRetry,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskOutput = MethodOutput.from("9")
        )
    }

    "Should be able to run an explicit method with 2 parameters without parameterTypes" {
        val input = arrayOf(4, "3")
        // with
        val msg = getTaskExecutorMessage("foo", "other", input, null)
        // when
        taskExecutor.register("foo") { TestingSampleTask() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskRetry = msg.taskRetry,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskOutput = MethodOutput.from("12")
        )
    }

    "Should throw ClassNotFoundDuringTaskInstantiation when trying to process an unknown task" {
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getTaskExecutorMessage("foo", "unknown", input, types)
        // when
        taskExecutor.unregister("foo")
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskRetry shouldBe msg.taskRetry
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.get()!!::class.java.name shouldBe ClassNotFoundDuringInstantiation::class.java.name
    }

    "Should throw NoMethodFoundWithParameterTypes when trying to process an unknown method" {
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getTaskExecutorMessage("foo", "unknown", input, types)
        // when
        taskExecutor.register("foo") { TestingSampleTask() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskRetry shouldBe msg.taskRetry
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.get()!!::class.java.name shouldBe NoMethodFoundWithParameterTypes::class.java.name
    }

    "Should throw NoMethodFoundWithParameterCount when trying to process an unknown method without parameterTypes" {
        val input = arrayOf(2, "3")
        // with
        val msg = getTaskExecutorMessage("foo", "unknown", input, null)
        // when
        taskExecutor.register("foo") { TestingSampleTask() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskRetry shouldBe msg.taskRetry
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.get()!!::class.java.name shouldBe NoMethodFoundWithParameterCount::class.java.name
    }

    "Should throw TooManyMethodsFoundWithParameterCount when trying to process an unknown method without parameterTypes" {
        val input = arrayOf(2, "3")
        // with
        val msg = getTaskExecutorMessage("foo", "handle", input, null)
        // when
        taskExecutor.register("foo") { TestingSampleTask() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskRetry shouldBe msg.taskRetry
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.get()!!::class.java.name shouldBe TooManyMethodsFoundWithParameterCount::class.java.name
    }

    "Should retry with correct exception" {
        val input = arrayOf(2, "3")
        // with
        val msg = getTaskExecutorMessage("foo", "handle", input, null)
        // when
        taskExecutor.register("foo") { SampleTaskWithRetry() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskRetry shouldBe msg.taskRetry
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe 3F
        fail.taskAttemptError.get()!!::class.java.name shouldBe IllegalStateException::class.java.name
    }

    "Should throw RetryDelayReturnTypeError when getRetryDelay has wrong return type" {
        val input = arrayOf(2, "3")
        // with
        val msg = getTaskExecutorMessage("foo", "handle", input, null)
        // when
        taskExecutor.register("foo") { SampleTaskWithBadTypeRetry() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskRetry shouldBe msg.taskRetry
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.get()!!::class.java.name shouldBe RetryDelayHasWrongReturnType::class.java.name
    }

    "Should throw when getRetryDelay throw an exception" {
        val input = arrayOf(2, "3")
        // with
        val msg = getTaskExecutorMessage("foo", "handle", input, null)
        // when
        taskExecutor.register("foo") { SampleTaskWithBuggyRetry() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskRetry shouldBe msg.taskRetry
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.get()!!::class.java.name shouldBe IllegalArgumentException::class.java.name
    }

    "Should be able to access context from task" {
        val input = arrayOf(2, "3")
        // with
        val msg = getTaskExecutorMessage("foo", "handle", input, null)
        // when
        taskExecutor.register("foo") { SampleTaskWithContext() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskRetry = msg.taskRetry,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskOutput = MethodOutput.from("72")
        )
    }

    "Should throw ProcessingTimeout if processing time is too long" {
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getTaskExecutorMessage("foo", "handle", input, types)
        // when
        taskExecutor.register("foo") { SampleTaskWithTimeout() }
        coroutineScope { taskExecutor.handle(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskRetry shouldBe msg.taskRetry
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.get()!!::class.java.name shouldBe ProcessingTimeout::class.java.name
    }
})

private fun getTaskExecutorMessage(name: String, method: String, input: Array<out Any?>, types: List<String>?) = TaskExecutorMessage(
    taskId = TaskId(),
    taskAttemptId = TaskAttemptId(),
    taskRetry = TaskRetry(12),
    taskAttemptRetry = TaskAttemptRetry(7),
    taskName = TaskName(name),
    methodName = MethodName(method),
    methodParameterTypes = types?.let { MethodParameterTypes(it) },
    methodInput = MethodInput.from(*input),
    lastTaskAttemptError = null,
    taskOptions = TaskOptions(runningTimeout = .2F),
    taskMeta = TaskMeta()
)

private fun getTaskAttemptStarted(msg: TaskExecutorMessage) = TaskAttemptStarted(
    taskId = msg.taskId,
    taskAttemptId = msg.taskAttemptId,
    taskRetry = msg.taskRetry,
    taskAttemptRetry = msg.taskAttemptRetry
)
