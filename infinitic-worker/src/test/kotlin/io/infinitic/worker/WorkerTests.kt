// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

@file:Suppress("unused")

package io.infinitic.worker

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskAttemptIndex
import io.infinitic.common.tasks.data.TaskAttemptRetry
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.MethodInput
import io.infinitic.common.tasks.data.MethodName
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.MethodOutput
import io.infinitic.common.tasks.data.MethodParameterTypes
import io.infinitic.common.tasks.exceptions.ClassNotFoundDuringInstantiation
import io.infinitic.common.tasks.exceptions.NoMethodFoundWithParameterCount
import io.infinitic.common.tasks.exceptions.NoMethodFoundWithParameterTypes
import io.infinitic.common.tasks.exceptions.ProcessingTimeout
import io.infinitic.common.tasks.exceptions.RetryDelayHasWrongReturnType
import io.infinitic.common.tasks.exceptions.TooManyMethodsFoundWithParameterCount
import io.infinitic.common.tasks.messages.taskEngineMessages.TaskAttemptCompleted
import io.infinitic.common.tasks.messages.taskEngineMessages.TaskAttemptFailed
import io.infinitic.common.tasks.messages.taskEngineMessages.TaskAttemptStarted
import io.infinitic.common.tasks.messages.taskEngineMessages.TaskEngineMessage
import io.infinitic.common.tasks.messages.workerMessages.RunTask
import io.infinitic.worker.samples.TestingSampleTask
import io.infinitic.worker.samples.SampleTaskWithRetry
import io.infinitic.worker.samples.SampleTaskWithBadTypeRetry
import io.infinitic.worker.samples.SampleTaskWithBuggyRetry
import io.infinitic.worker.samples.SampleTaskWithContext
import io.infinitic.worker.samples.SampleTaskWithTimeout
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import io.kotest.matchers.types.shouldBeInstanceOf

class WorkerTests : StringSpec({
    val dispatcher = mockk<Dispatcher>()
    val slots = mutableListOf<TaskEngineMessage>()
    coEvery { dispatcher.toTaskEngine(capture(slots)) } just Runs
    val worker = Worker(dispatcher)

    // ensure slots are emptied between each test
    beforeTest {
        slots.clear()
    }

    "Should be able to run an explicit method with 2 parameters" {
        val input = arrayOf(3, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask("foo", "other", input, types)
        // when
        worker.register("foo") { TestingSampleTask() }
        coroutineScope { worker.runTask(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskOutput = MethodOutput.from("9")
        )
    }

    "Should be able to run an explicit method with 2 parameters without parameterTypes" {
        val input = arrayOf(4, "3")
        // with
        val msg = getRunTask("foo", "other", input, null)
        // when
        worker.register("foo") { TestingSampleTask() }
        coroutineScope { worker.runTask(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskOutput = MethodOutput.from("12")
        )
    }

    "Should throw ClassNotFoundDuringTaskInstantiation when trying to process an unknown task" {
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask("foo", "unknown", input, types)
        // when
        worker.unregister("foo")
        coroutineScope { worker.runTask(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.get()!!::class.java.name shouldBe ClassNotFoundDuringInstantiation::class.java.name
    }

    "Should throw NoMethodFoundWithParameterTypes when trying to process an unknown method" {
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask("foo", "unknown", input, types)
        // when
        worker.register("foo") { TestingSampleTask() }
        coroutineScope { worker.runTask(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.get()!!::class.java.name shouldBe NoMethodFoundWithParameterTypes::class.java.name
    }

    "Should throw NoMethodFoundWithParameterCount when trying to process an unknown method without parameterTypes" {
        val input = arrayOf(2, "3")
        // with
        val msg = getRunTask("foo", "unknown", input, null)
        // when
        worker.register("foo") { TestingSampleTask() }
        coroutineScope { worker.runTask(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.get()!!::class.java.name shouldBe NoMethodFoundWithParameterCount::class.java.name
    }

    "Should throw TooManyMethodsFoundWithParameterCount when trying to process an unknown method without parameterTypes" {
        val input = arrayOf(2, "3")
        // with
        val msg = getRunTask("foo", "handle", input, null)
        // when
        worker.register("foo") { TestingSampleTask() }
        coroutineScope { worker.runTask(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.get()!!::class.java.name shouldBe TooManyMethodsFoundWithParameterCount::class.java.name
    }

    "Should retry with correct exception" {
        val input = arrayOf(2, "3")
        // with
        val msg = getRunTask("foo", "handle", input, null)
        // when
        worker.register("foo") { SampleTaskWithRetry() }
        coroutineScope { worker.runTask(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe 3F
        fail.taskAttemptError.get()!!::class.java.name shouldBe IllegalStateException::class.java.name
    }

    "Should throw RetryDelayReturnTypeError when getRetryDelay has wrong return type" {
        val input = arrayOf(2, "3")
        // with
        val msg = getRunTask("foo", "handle", input, null)
        // when
        worker.register("foo") { SampleTaskWithBadTypeRetry() }
        coroutineScope { worker.runTask(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.get()!!::class.java.name shouldBe RetryDelayHasWrongReturnType::class.java.name
    }

    "Should throw when getRetryDelay throw an exception" {
        val input = arrayOf(2, "3")
        // with
        val msg = getRunTask("foo", "handle", input, null)
        // when
        worker.register("foo") { SampleTaskWithBuggyRetry() }
        coroutineScope { worker.runTask(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.get()!!::class.java.name shouldBe IllegalArgumentException::class.java.name
    }

    "Should be able to access context from task" {
        val input = arrayOf(2, "3")
        // with
        val msg = getRunTask("foo", "handle", input, null)
        // when
        worker.register("foo") { SampleTaskWithContext() }
        coroutineScope { worker.runTask(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskOutput = MethodOutput.from("72")
        )
    }

    "Should throw ProcessingTimeout if processing time is too long" {
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask("foo", "handle", input, types)
        // when
        worker.register("foo") { SampleTaskWithTimeout() }
        coroutineScope { worker.runTask(msg) }
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1].shouldBeInstanceOf<TaskAttemptFailed>()
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.get()!!::class.java.name shouldBe ProcessingTimeout::class.java.name
    }
})

private fun getRunTask(name: String, method: String, input: Array<out Any?>, types: List<String>?) = RunTask(
    taskId = TaskId(),
    taskAttemptId = TaskAttemptId(),
    taskAttemptIndex = TaskAttemptIndex(12),
    taskAttemptRetry = TaskAttemptRetry(7),
    taskName = TaskName(name),
    methodName = MethodName(method),
    methodParameterTypes = MethodParameterTypes(types),
    methodInput = MethodInput.from(*input),
    taskOptions = TaskOptions(runningTimeout = .2F),
    taskMeta = TaskMeta()
)

private fun getTaskAttemptStarted(msg: RunTask) = TaskAttemptStarted(
    taskId = msg.taskId,
    taskAttemptId = msg.taskAttemptId,
    taskAttemptIndex = msg.taskAttemptIndex,
    taskAttemptRetry = msg.taskAttemptRetry
)
