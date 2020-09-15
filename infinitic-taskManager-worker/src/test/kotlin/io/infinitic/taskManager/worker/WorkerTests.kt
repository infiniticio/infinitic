@file:Suppress("unused")

package io.infinitic.taskManager.worker

import io.infinitic.taskManager.common.data.TaskAttemptContext
import io.infinitic.taskManager.common.data.TaskAttemptId
import io.infinitic.taskManager.common.data.TaskAttemptIndex
import io.infinitic.taskManager.common.data.TaskAttemptRetry
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.taskManager.common.data.TaskOptions
import io.infinitic.taskManager.common.data.TaskOutput
import io.infinitic.taskManager.common.exceptions.ClassNotFoundDuringInstantiation
import io.infinitic.taskManager.common.exceptions.ErrorDuringInstantiation
import io.infinitic.taskManager.common.exceptions.InvalidUseOfDividerInTaskName
import io.infinitic.taskManager.common.exceptions.MultipleUseOfDividerInTaskName
import io.infinitic.taskManager.common.exceptions.NoMethodFoundWithParameterCount
import io.infinitic.taskManager.common.exceptions.NoMethodFoundWithParameterTypes
import io.infinitic.taskManager.common.exceptions.ProcessingTimeout
import io.infinitic.taskManager.common.exceptions.RetryDelayHasWrongReturnType
import io.infinitic.taskManager.common.exceptions.TooManyMethodsFoundWithParameterCount
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage
import io.infinitic.taskManager.common.messages.RunTask
import io.infinitic.taskManager.common.messages.TaskAttemptCompleted
import io.infinitic.taskManager.common.messages.TaskAttemptFailed
import io.infinitic.taskManager.common.messages.TaskAttemptStarted
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.launch

class WorkerTests : StringSpec({
    val dispatcher = mockk<WorkerDispatcher>()
    val slots = mutableListOf<ForTaskEngineMessage>()
    coEvery { dispatcher.toTaskEngine(capture(slots)) } just Runs
    val worker = Worker(dispatcher)

    // ensure slots are emptied between each test
    beforeTest {
        slots.clear()
    }

    "Should be able to run a default method with 2 parameters" {
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask(TestWithoutRetry::class.java.name, input, types)
        // when
        launch { worker.runTask(msg) }.join()
        // then
        slots.size shouldBe 2

        slots[0] shouldBe getTaskAttemptStarted(msg)

        slots[1] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskOutput = TaskOutput("6")
        )
    }

    "Should be able to run an explicit method with 2 parameters" {
        val input = arrayOf(3, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask("${TestWithoutRetryAndExplicitMethod::class.java.name}::run", input, types)
        // when
        launch { worker.runTask(msg) }.join()
        // then
        slots.size shouldBe 2

        slots[0] shouldBe getTaskAttemptStarted(msg)

        slots[1] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskOutput = TaskOutput("9")
        )
    }

    "Should be able to run an explicit method with 2 parameters without parameterTypes" {
        val input = arrayOf(4, "3")
        // with
        val msg = getRunTask("${TestWithoutRetryAndExplicitMethod::class.java.name}::run", input, null)
        // when
        launch { worker.runTask(msg) }.join()
        // then
        slots.size shouldBe 2

        slots[0] shouldBe getTaskAttemptStarted(msg)

        slots[1] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskOutput = TaskOutput("12")
        )
    }

    "Should fail when trying to register an invalid task name " {
        shouldThrow<InvalidUseOfDividerInTaskName> {
            Worker.register("blabla::", TestWithoutRetry())
        }
    }

    "Should be able to use a registered task name " {
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask("blabla", input, types)
        // when
        Worker.register("blabla", TestWithoutRetry())
        launch { worker.runTask(msg) }.join()
        // then
        slots.size shouldBe 2

        slots[0] shouldBe getTaskAttemptStarted(msg)

        slots[1] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskOutput = TaskOutput("6")
        )
    }

    "Should throw _root_ide_package_.io.infinitic.taskManager.common.exceptions.MultipleUseOfDividerInTaskNameme when trying to process an invalid task name " {
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask("blabla::m1::m2", input, types)
        // when
        launch { worker.runTask(msg) }.join()
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        (slots[1] is TaskAttemptFailed) shouldBe true
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.data!!::class.java.name shouldBe MultipleUseOfDividerInTaskName::class.java.name
    }

    "Should throw ClassNotFoundDuringTaskInstantiation when trying to process an unknown task" {
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask("blabla", input, types)
        // when
        Worker.unregister("blabla")
        launch { worker.runTask(msg) }.join()
        // then
        slots.size shouldBe 2

        slots[0] shouldBe getTaskAttemptStarted(msg)

        (slots[1] is TaskAttemptFailed) shouldBe true
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.data!!::class.java.name shouldBe ClassNotFoundDuringInstantiation::class.java.name
    }

    "Should throw ErrorDuringTaskInstantiation when if impossible to create new instance" {
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask(TestWithConstructor::class.java.name, input, types)
        // when
        launch { worker.runTask(msg) }.join()
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        (slots[1] is TaskAttemptFailed) shouldBe true
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.data!!::class.java.name shouldBe ErrorDuringInstantiation::class.java.name
    }

    "Should throw NoMethodFoundWithParameterTypes  when trying to process an unknown method" {
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask("${TestWithoutRetry::class.java.name}::unknown", input, types)
        // when
        launch { worker.runTask(msg) }.join()
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        (slots[1] is TaskAttemptFailed) shouldBe true
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.data!!::class.java.name shouldBe NoMethodFoundWithParameterTypes::class.java.name
    }

    "Should throw NoMethodFoundWithParameterCount when trying to process an unknown method without parameterTypes" {
        val input = arrayOf(2, "3")
        // with
        val msg = getRunTask("${TestWithoutRetry::class.java.name}::unknown", input, null)
        // when
        launch { worker.runTask(msg) }.join()
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        (slots[1] is TaskAttemptFailed) shouldBe true
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.data!!::class.java.name shouldBe NoMethodFoundWithParameterCount::class.java.name
    }

    "Should throw TooManyMethodsFoundWithParameterCount when trying to process an unknown method without parameterTypes" {
        val input = arrayOf(2, "3")
        // with
        val msg = getRunTask("${TestWithoutRetry::class.java.name}::handle", input, null)
        // when
        launch { worker.runTask(msg) }.join()
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        (slots[1] is TaskAttemptFailed) shouldBe true
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.data!!::class.java.name shouldBe TooManyMethodsFoundWithParameterCount::class.java.name
    }

    "Should retry with correct exception" {
        val input = arrayOf(2, "3")
        // with
        val msg = getRunTask(TestWithRetry::class.java.name, input, null)
        // when
        launch { worker.runTask(msg) }.join()
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        (slots[1] is TaskAttemptFailed) shouldBe true
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe 3F
        fail.taskAttemptError.data!!::class.java.name shouldBe IllegalStateException::class.java.name
    }

    "Should throw RetryDelayReturnTypeError when getRetryDelay has wrong return type" {
        val input = arrayOf(2, "3")
        // with
        val msg = getRunTask(TestWithBadRetryType::class.java.name, input, null)
        // when
        launch { worker.runTask(msg) }.join()
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        (slots[1] is TaskAttemptFailed) shouldBe true
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.data!!::class.java.name shouldBe RetryDelayHasWrongReturnType::class.java.name
    }

    "Should throw when getRetryDelay throw an exception" {
        val input = arrayOf(2, "3")
        // with
        val msg = getRunTask(TestWithBuggyRetry::class.java.name, input, null)
        // when
        launch { worker.runTask(msg) }.join()
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        (slots[1] is TaskAttemptFailed) shouldBe true
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.data!!::class.java.name shouldBe IllegalArgumentException::class.java.name
    }

    "Should be able to access context from task" {
        val input = arrayOf(2, "3")
        // with
        val msg = getRunTask(TestWithContext::class.java.name, input, null)
        // when
        launch { worker.runTask(msg) }.join()
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        slots[1] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskOutput = TaskOutput("72")
        )
    }

    "Should throw ProcessingTimeout if processing time is too long" {
        val input = arrayOf(2, "3")
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask(TestWithTimeout::class.java.name, input, types)
        // when
        launch { worker.runTask(msg) }.join()
        // then
        slots.size shouldBe 2
        slots[0] shouldBe getTaskAttemptStarted(msg)
        (slots[1] is TaskAttemptFailed) shouldBe true
        val fail = slots[1] as TaskAttemptFailed
        fail.taskId shouldBe msg.taskId
        fail.taskAttemptId shouldBe msg.taskAttemptId
        fail.taskAttemptIndex shouldBe msg.taskAttemptIndex
        fail.taskAttemptRetry shouldBe msg.taskAttemptRetry
        fail.taskAttemptDelayBeforeRetry shouldBe null
        fail.taskAttemptError.data!!::class.java.name shouldBe ProcessingTimeout::class.java.name
    }
})

internal class TestWithoutRetry {
    fun handle(i: Int, j: String) = (i * j.toInt()).toString()
    fun handle(i: Int, j: Int) = (i * j).toString()
}

internal class TestWithoutRetryAndExplicitMethod {
    fun run(i: Int, j: String) = (i * j.toInt()).toString()
}

internal class TestWithRetry {
    lateinit var context: TaskAttemptContext

    fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()
    fun getRetryDelay(): Float? = if (context.exception is IllegalStateException) 3F else 0F
}

internal class TestWithBuggyRetry {
    lateinit var context: TaskAttemptContext

    fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()
    fun getRetryDelay(): Float? = if (context.exception is IllegalStateException) throw IllegalArgumentException() else 3F
}

internal class TestWithBadRetryType {
    fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()
    fun getRetryDelay() = 3
}

internal class TestWithConstructor(val value: String) {
    fun handle(i: Int, j: String) = (i * j.toInt()).toString()
}

internal class TestWithContext() {
    private lateinit var context: TaskAttemptContext

    fun handle(i: Int, j: String) = (i * j.toInt() * context.taskAttemptIndex.int).toString()
}

internal class TestWithTimeout() {
    lateinit var context: TaskAttemptContext

    fun handle(i: Int, j: String): String {
        Thread.sleep(400)

        return (i * j.toInt() * context.taskAttemptIndex.int).toString()
    }
}

private fun getRunTask(name: String, input: Array<out Any?>, types: List<String>?) = RunTask(
    taskId = TaskId(),
    taskAttemptId = TaskAttemptId(),
    taskAttemptIndex = TaskAttemptIndex(12),
    taskAttemptRetry = TaskAttemptRetry(7),
    taskName = TaskName(name),
    taskInput = TaskInput(*input),
    taskOptions = TaskOptions(runningTimeout = .2F),
    taskMeta = TaskMeta().withParameterTypes<TaskMeta>(types)
)

private fun getTaskAttemptStarted(msg: RunTask) = TaskAttemptStarted(
    taskId = msg.taskId,
    taskAttemptId = msg.taskAttemptId,
    taskAttemptIndex = msg.taskAttemptIndex,
    taskAttemptRetry = msg.taskAttemptRetry
)
