package com.zenaton.taskManager.worker

import com.zenaton.common.data.SerializedData
import com.zenaton.taskManager.common.data.TaskAttemptContext
import com.zenaton.taskManager.common.data.TaskAttemptId
import com.zenaton.taskManager.common.data.TaskAttemptIndex
import com.zenaton.taskManager.common.data.TaskAttemptRetry
import com.zenaton.taskManager.common.data.TaskId
import com.zenaton.taskManager.common.data.TaskInput
import com.zenaton.taskManager.common.data.TaskMeta
import com.zenaton.taskManager.common.data.TaskName
import com.zenaton.taskManager.common.data.TaskOptions
import com.zenaton.taskManager.common.data.TaskOutput
import com.zenaton.taskManager.common.exceptions.ClassNotFoundDuringTaskInstantiation
import com.zenaton.taskManager.common.exceptions.ErrorDuringTaskInstantiation
import com.zenaton.taskManager.common.exceptions.InvalidUseOfDividerInTaskName
import com.zenaton.taskManager.common.exceptions.MultipleUseOfDividerInTaskName
import com.zenaton.taskManager.common.exceptions.NoMethodFoundWithParameterCount
import com.zenaton.taskManager.common.exceptions.NoMethodFoundWithParameterTypes
import com.zenaton.taskManager.common.exceptions.ProcessingTimeout
import com.zenaton.taskManager.common.exceptions.RetryDelayHasWrongReturnType
import com.zenaton.taskManager.common.exceptions.TaskAttemptContextRetrievedOutsideOfProcessingThread
import com.zenaton.taskManager.common.exceptions.TooManyMethodsFoundWithParameterCount
import com.zenaton.taskManager.common.messages.ForTaskEngineMessage
import com.zenaton.taskManager.common.messages.RunTask
import com.zenaton.taskManager.common.messages.TaskAttemptCompleted
import com.zenaton.taskManager.common.messages.TaskAttemptFailed
import com.zenaton.taskManager.common.messages.TaskAttemptStarted
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope

class WorkerTests : StringSpec({
    val dispatcher = mockk<Dispatcher>()
    val slots = mutableListOf<ForTaskEngineMessage>()
    every { dispatcher.toTaskEngine(capture(slots)) } just Runs
    val worker = Worker()
    worker.dispatcher = dispatcher

    // ensure slots are emptied between each test
    beforeTest {
        slots.clear()
    }

    "Should be able to run a default method with 2 parameters" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask(TestWithoutRetry::class.java.name, input, types)
        // when
        coroutineScope {
            worker.handle(msg)
        }
        // then
        slots.size shouldBe 2

        slots[0] shouldBe getTaskAttemptStarted(msg)

        slots[1] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskOutput = TaskOutput(SerializedData.from("6"))
        )
    }

    "Should be able to run an explicit method with 2 parameters" {
        val input = listOf(3, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask("${TestWithoutRetryAndExplicitMethod::class.java.name}::run", input, types)
        // when
        coroutineScope {
            worker.handle(msg)
        }

        // then
        slots.size shouldBe 2

        slots[0] shouldBe getTaskAttemptStarted(msg)

        slots[1] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskOutput = TaskOutput(SerializedData.from("9"))
        )
    }

    "Should be able to run an explicit method with 2 parameters without parameterTypes" {
        val input = listOf(4, "3").map { SerializedData.from(it) }
        // with
        val msg = getRunTask("${TestWithoutRetryAndExplicitMethod::class.java.name}::run", input, null)
        // when
        coroutineScope {
            worker.handle(msg)
        }

        // then
        slots.size shouldBe 2

        slots[0] shouldBe getTaskAttemptStarted(msg)

        slots[1] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskOutput = TaskOutput(SerializedData.from("12"))
        )
    }

    "Should fail when trying to register an invalid task name " {
        shouldThrow<InvalidUseOfDividerInTaskName> {
            Worker.register("blabla::", TestWithoutRetry())
        }
    }

    "Should be able to use a registered task name " {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask("blabla", input, types)
        // when
        Worker.register("blabla", TestWithoutRetry())

        coroutineScope {
            worker.handle(msg)
        }
        // then
        slots.size shouldBe 2

        slots[0] shouldBe getTaskAttemptStarted(msg)

        slots[1] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskOutput = TaskOutput(SerializedData.from("6"))
        )
    }

    "Should throw _root_ide_package_.com.zenaton.taskManager.common.exceptions.MultipleUseOfDividerInTaskNameme when trying to process an invalid task name " {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask("blabla::m1::m2", input, types)

        // when
        coroutineScope {
            worker.handle(msg)
        }

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
        fail.taskAttemptError.error.deserialize()!!::class.java.name shouldBe MultipleUseOfDividerInTaskName::class.java.name
    }

    "Should throw ClassNotFoundDuringJobInstantiation when trying to process an unknown task" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask("blabla", input, types)

        // when
        Worker.unregister("blabla")

        coroutineScope {
            worker.handle(msg)
        }

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
        fail.taskAttemptError.error.deserialize()!!::class.java.name shouldBe ClassNotFoundDuringTaskInstantiation::class.java.name
    }

    "Should throw ErrorDuringJobInstantiation when if impossible to create new instance" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask(TestWithConstructor::class.java.name, input, types)

        // when
        coroutineScope {
            worker.handle(msg)
        }

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
        fail.taskAttemptError.error.deserialize()!!::class.java.name shouldBe ErrorDuringTaskInstantiation::class.java.name
    }

    "Should throw NoMethodFoundWithParameterTypes  when trying to process an unknown method" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunTask("${TestWithoutRetry::class.java.name}::unknown", input, types)

        // when
        coroutineScope {
            worker.handle(msg)
        }

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
        fail.taskAttemptError.error.deserialize()!!::class.java.name shouldBe NoMethodFoundWithParameterTypes::class.java.name
    }

    "Should throw NoMethodFoundWithParameterCount when trying to process an unknown method without parameterTypes" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        // with
        val msg = getRunTask("${TestWithoutRetry::class.java.name}::unknown", input, null)

        // when
        coroutineScope {
            worker.handle(msg)
        }

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
        fail.taskAttemptError.error.deserialize()!!::class.java.name shouldBe NoMethodFoundWithParameterCount::class.java.name
    }

    "Should throw TooManyMethodsFoundWithParameterCount when trying to process an unknown method without parameterTypes" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        // with
        val msg = getRunTask("${TestWithoutRetry::class.java.name}::handle", input, null)

        // when
        coroutineScope {
            worker.handle(msg)
        }

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
        fail.taskAttemptError.error.deserialize()!!::class.java.name shouldBe TooManyMethodsFoundWithParameterCount::class.java.name
    }

    "Should retry with correct exception" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        // with
        val msg = getRunTask(TestWithRetry::class.java.name, input, null)

        // when
        coroutineScope {
            worker.handle(msg)
        }

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
        fail.taskAttemptError.error.deserialize()!!::class.java.name shouldBe IllegalStateException::class.java.name
    }

    "Should throw RetryDelayReturnTypeError when getRetryDelay has wrong return type" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        // with
        val msg = getRunTask(TestWithBadRetryType::class.java.name, input, null)

        // when
        coroutineScope {
            worker.handle(msg)
        }

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
        fail.taskAttemptError.error.deserialize()!!::class.java.name shouldBe RetryDelayHasWrongReturnType::class.java.name
    }

    "Should throw when getRetryDelay throw an exception" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        // with
        val msg = getRunTask(TestWithBuggyRetry::class.java.name, input, null)

        // when
        coroutineScope {
            worker.handle(msg)
        }

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
        fail.taskAttemptError.error.deserialize()!!::class.java.name shouldBe IllegalArgumentException::class.java.name
    }

    "Should be able to access context from task" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        // with
        val msg = getRunTask(TestWithContext::class.java.name, input, null)

        // when
        coroutineScope {
            worker.handle(msg)
        }

        // then
        slots.size shouldBe 2

        slots[0] shouldBe getTaskAttemptStarted(msg)

        slots[1] shouldBe TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskOutput = TaskOutput(SerializedData.from("72"))
        )
    }

    "Should throw when Worker.getContext() called outside of a processing thread " {
        shouldThrow<TaskAttemptContextRetrievedOutsideOfProcessingThread> {
            Worker.context
        }
    }

    "Should throw ProcessingTimeout if processing time is too long" {
        val types = listOf(Int::class.java.name, String::class.java.name)
        val input = listOf(2, "3").map { SerializedData.from(it) }
        // with
        val msg = getRunTask(TestWithTimeout::class.java.name, input, types)

        // when
        coroutineScope {
            worker.handle(msg)
        }

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
        fail.taskAttemptError.error.deserialize()!!::class.java.name shouldBe ProcessingTimeout::class.java.name
    }
})

internal class TestWithoutRetry {
    @Suppress("unused") fun handle(i: Int, j: String) = (i * j.toInt()).toString()
    @Suppress("unused") fun handle(i: Int, j: Int) = (i * j).toString()
}

internal class TestWithoutRetryAndExplicitMethod {
    @Suppress("unused") fun run(i: Int, j: String) = (i * j.toInt()).toString()
}

internal class TestWithRetry {
    @Suppress("unused") fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()
    @Suppress("unused") fun getRetryDelay(context: TaskAttemptContext): Float? = if (context.exception is IllegalStateException) 3F else 0F
}

internal class TestWithBuggyRetry {
    @Suppress("unused") fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()
    @Suppress("unused") fun getRetryDelay(context: TaskAttemptContext): Float? = if (context.exception is IllegalStateException) throw IllegalArgumentException() else 3F
}

internal class TestWithBadRetryType {
    @Suppress("unused") fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()
    @Suppress("unused") fun getRetryDelay(context: TaskAttemptContext) = 3
}

internal class TestWithConstructor(val value: String) {
    @Suppress("unused") fun handle(i: Int, j: String) = (i * j.toInt()).toString()
}

internal class TestWithContext() {
    @Suppress("unused") fun handle(i: Int, j: String) = (i * j.toInt() * Worker.context.taskAttemptIndex.int).toString()
}

internal class TestWithTimeout() {
    @Suppress("unused") fun handle(i: Int, j: String): String {
        Thread.sleep(400)

        return (i * j.toInt() * Worker.context.taskAttemptIndex.int).toString()
    }
}

private fun getRunTask(name: String, input: List<SerializedData>, types: List<String>?) = RunTask(
    taskId = TaskId(),
    taskAttemptId = TaskAttemptId(),
    taskAttemptIndex = TaskAttemptIndex(12),
    taskAttemptRetry = TaskAttemptRetry(7),
    taskName = TaskName(name),
    taskInput = TaskInput(input),
    taskOptions = TaskOptions(runningTimeout = .2F),
    taskMeta = TaskMeta().setParameterTypes(types)
)

private fun getTaskAttemptStarted(msg: RunTask) = TaskAttemptStarted(
    taskId = msg.taskId,
    taskAttemptId = msg.taskAttemptId,
    taskAttemptIndex = msg.taskAttemptIndex,
    taskAttemptRetry = msg.taskAttemptRetry
)
