package com.zenaton.taskManager.worker

import com.zenaton.common.data.SerializedData
import com.zenaton.taskManager.common.data.JobAttemptContext
import com.zenaton.taskManager.common.data.JobAttemptId
import com.zenaton.taskManager.common.data.JobAttemptIndex
import com.zenaton.taskManager.common.data.JobAttemptRetry
import com.zenaton.taskManager.common.data.JobId
import com.zenaton.taskManager.common.data.JobInput
import com.zenaton.taskManager.common.data.JobMeta
import com.zenaton.taskManager.common.data.JobName
import com.zenaton.taskManager.common.data.JobOptions
import com.zenaton.taskManager.common.data.JobOutput
import com.zenaton.taskManager.common.exceptions.ClassNotFoundDuringJobInstantiation
import com.zenaton.taskManager.common.exceptions.ErrorDuringJobInstantiation
import com.zenaton.taskManager.common.exceptions.InvalidUseOfDividerInJobName
import com.zenaton.taskManager.common.exceptions.JobAttemptContextRetrievedOutsideOfProcessingThread
import com.zenaton.taskManager.common.exceptions.MultipleUseOfDividerInJobName
import com.zenaton.taskManager.common.exceptions.NoMethodFoundWithParameterCount
import com.zenaton.taskManager.common.exceptions.NoMethodFoundWithParameterTypes
import com.zenaton.taskManager.common.exceptions.ProcessingTimeout
import com.zenaton.taskManager.common.exceptions.RetryDelayHasWrongReturnType
import com.zenaton.taskManager.common.exceptions.TooManyMethodsFoundWithParameterCount
import com.zenaton.taskManager.common.messages.ForJobEngineMessage
import com.zenaton.taskManager.common.messages.JobAttemptCompleted
import com.zenaton.taskManager.common.messages.JobAttemptFailed
import com.zenaton.taskManager.common.messages.JobAttemptStarted
import com.zenaton.taskManager.common.messages.RunJob
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import java.lang.IllegalStateException

class WorkerTests : StringSpec({
    val dispatcher = mockk<Dispatcher>()
    val slots = mutableListOf<ForJobEngineMessage>()
    every { dispatcher.toJobEngine(capture(slots)) } just Runs
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
        val msg = getRunJob(TestWithoutRetry::class.java.name, input, types)
        // when
        coroutineScope {
            worker.handle(msg)
        }
        // then
        slots.size shouldBe 2

        slots[0] shouldBe getJobAttemptStarted(msg)

        slots[1] shouldBe JobAttemptCompleted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptIndex = msg.jobAttemptIndex,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobOutput = JobOutput(SerializedData.from("6"))
        )
    }

    "Should be able to run an explicit method with 2 parameters" {
        val input = listOf(3, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunJob("${TestWithoutRetryAndExplicitMethod::class.java.name}::run", input, types)
        // when
        coroutineScope {
            worker.handle(msg)
        }

        // then
        slots.size shouldBe 2

        slots[0] shouldBe getJobAttemptStarted(msg)

        slots[1] shouldBe JobAttemptCompleted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptIndex = msg.jobAttemptIndex,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobOutput = JobOutput(SerializedData.from("9"))
        )
    }

    "Should be able to run an explicit method with 2 parameters without parameterTypes" {
        val input = listOf(4, "3").map { SerializedData.from(it) }
        // with
        val msg = getRunJob("${TestWithoutRetryAndExplicitMethod::class.java.name}::run", input, null)
        // when
        coroutineScope {
            worker.handle(msg)
        }

        // then
        slots.size shouldBe 2

        slots[0] shouldBe getJobAttemptStarted(msg)

        slots[1] shouldBe JobAttemptCompleted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptIndex = msg.jobAttemptIndex,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobOutput = JobOutput(SerializedData.from("12"))
        )
    }

    "Should fail when trying to register an invalid task name " {
        shouldThrow<InvalidUseOfDividerInJobName> {
            Worker.register("blabla::", TestWithoutRetry())
        }
    }

    "Should be able to use a registered task name " {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunJob("blabla", input, types)
        // when
        Worker.register("blabla", TestWithoutRetry())

        coroutineScope {
            worker.handle(msg)
        }
        // then
        slots.size shouldBe 2

        slots[0] shouldBe getJobAttemptStarted(msg)

        slots[1] shouldBe JobAttemptCompleted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptIndex = msg.jobAttemptIndex,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobOutput = JobOutput(SerializedData.from("6"))
        )
    }

    "Should throw MultipleUseOfDividerInJobName when trying to process an invalid task name " {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunJob("blabla::m1::m2", input, types)

        // when
        coroutineScope {
            worker.handle(msg)
        }

        // then
        slots.size shouldBe 2

        slots[0] shouldBe getJobAttemptStarted(msg)

        (slots[1] is JobAttemptFailed) shouldBe true
        val fail = slots[1] as JobAttemptFailed
        fail.jobId shouldBe msg.jobId
        fail.jobAttemptId shouldBe msg.jobAttemptId
        fail.jobAttemptIndex shouldBe msg.jobAttemptIndex
        fail.jobAttemptRetry shouldBe msg.jobAttemptRetry
        fail.jobAttemptDelayBeforeRetry shouldBe null
        fail.jobAttemptError.error.deserialize()!!::class.java.name shouldBe MultipleUseOfDividerInJobName::class.java.name
    }

    "Should throw ClassNotFoundDuringJobInstantiation when trying to process an unknown task" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunJob("blabla", input, types)

        // when
        Worker.unregister("blabla")

        coroutineScope {
            worker.handle(msg)
        }

        // then
        slots.size shouldBe 2

        slots[0] shouldBe getJobAttemptStarted(msg)

        (slots[1] is JobAttemptFailed) shouldBe true
        val fail = slots[1] as JobAttemptFailed
        fail.jobId shouldBe msg.jobId
        fail.jobAttemptId shouldBe msg.jobAttemptId
        fail.jobAttemptIndex shouldBe msg.jobAttemptIndex
        fail.jobAttemptRetry shouldBe msg.jobAttemptRetry
        fail.jobAttemptDelayBeforeRetry shouldBe null
        fail.jobAttemptError.error.deserialize()!!::class.java.name shouldBe ClassNotFoundDuringJobInstantiation::class.java.name
    }

    "Should throw ErrorDuringJobInstantiation when if impossible to create new instance" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunJob(TestWithConstructor::class.java.name, input, types)

        // when
        coroutineScope {
            worker.handle(msg)
        }

        // then
        slots.size shouldBe 2

        slots[0] shouldBe getJobAttemptStarted(msg)

        (slots[1] is JobAttemptFailed) shouldBe true
        val fail = slots[1] as JobAttemptFailed
        fail.jobId shouldBe msg.jobId
        fail.jobAttemptId shouldBe msg.jobAttemptId
        fail.jobAttemptIndex shouldBe msg.jobAttemptIndex
        fail.jobAttemptRetry shouldBe msg.jobAttemptRetry
        fail.jobAttemptDelayBeforeRetry shouldBe null
        fail.jobAttemptError.error.deserialize()!!::class.java.name shouldBe ErrorDuringJobInstantiation::class.java.name
    }

    "Should throw NoMethodFoundWithParameterTypes  when trying to process an unknown method" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunJob("${TestWithoutRetry::class.java.name}::unknown", input, types)

        // when
        coroutineScope {
            worker.handle(msg)
        }

        // then
        slots.size shouldBe 2

        slots[0] shouldBe getJobAttemptStarted(msg)

        (slots[1] is JobAttemptFailed) shouldBe true
        val fail = slots[1] as JobAttemptFailed
        fail.jobId shouldBe msg.jobId
        fail.jobAttemptId shouldBe msg.jobAttemptId
        fail.jobAttemptIndex shouldBe msg.jobAttemptIndex
        fail.jobAttemptRetry shouldBe msg.jobAttemptRetry
        fail.jobAttemptDelayBeforeRetry shouldBe null
        fail.jobAttemptError.error.deserialize()!!::class.java.name shouldBe NoMethodFoundWithParameterTypes::class.java.name
    }

    "Should throw NoMethodFoundWithParameterCount when trying to process an unknown method without parameterTypes" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        // with
        val msg = getRunJob("${TestWithoutRetry::class.java.name}::unknown", input, null)

        // when
        coroutineScope {
            worker.handle(msg)
        }

        // then
        slots.size shouldBe 2

        slots[0] shouldBe getJobAttemptStarted(msg)

        (slots[1] is JobAttemptFailed) shouldBe true
        val fail = slots[1] as JobAttemptFailed
        fail.jobId shouldBe msg.jobId
        fail.jobAttemptId shouldBe msg.jobAttemptId
        fail.jobAttemptIndex shouldBe msg.jobAttemptIndex
        fail.jobAttemptRetry shouldBe msg.jobAttemptRetry
        fail.jobAttemptDelayBeforeRetry shouldBe null
        fail.jobAttemptError.error.deserialize()!!::class.java.name shouldBe NoMethodFoundWithParameterCount::class.java.name
    }

    "Should throw TooManyMethodsFoundWithParameterCount when trying to process an unknown method without parameterTypes" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        // with
        val msg = getRunJob("${TestWithoutRetry::class.java.name}::handle", input, null)

        // when
        coroutineScope {
            worker.handle(msg)
        }

        // then
        slots.size shouldBe 2

        slots[0] shouldBe getJobAttemptStarted(msg)

        (slots[1] is JobAttemptFailed) shouldBe true
        val fail = slots[1] as JobAttemptFailed
        fail.jobId shouldBe msg.jobId
        fail.jobAttemptId shouldBe msg.jobAttemptId
        fail.jobAttemptIndex shouldBe msg.jobAttemptIndex
        fail.jobAttemptRetry shouldBe msg.jobAttemptRetry
        fail.jobAttemptDelayBeforeRetry shouldBe null
        fail.jobAttemptError.error.deserialize()!!::class.java.name shouldBe TooManyMethodsFoundWithParameterCount::class.java.name
    }

    "Should retry with correct exception" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        // with
        val msg = getRunJob(TestWithRetry::class.java.name, input, null)

        // when
        coroutineScope {
            worker.handle(msg)
        }

        // then
        slots.size shouldBe 2

        slots[0] shouldBe getJobAttemptStarted(msg)

        (slots[1] is JobAttemptFailed) shouldBe true
        val fail = slots[1] as JobAttemptFailed
        fail.jobId shouldBe msg.jobId
        fail.jobAttemptId shouldBe msg.jobAttemptId
        fail.jobAttemptIndex shouldBe msg.jobAttemptIndex
        fail.jobAttemptRetry shouldBe msg.jobAttemptRetry
        fail.jobAttemptDelayBeforeRetry shouldBe 3F
        fail.jobAttemptError.error.deserialize()!!::class.java.name shouldBe IllegalStateException::class.java.name
    }

    "Should throw RetryDelayReturnTypeError when getRetryDelay has wrong return type" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        // with
        val msg = getRunJob(TestWithBadRetryType::class.java.name, input, null)

        // when
        coroutineScope {
            worker.handle(msg)
        }

        // then
        slots.size shouldBe 2

        slots[0] shouldBe getJobAttemptStarted(msg)

        (slots[1] is JobAttemptFailed) shouldBe true
        val fail = slots[1] as JobAttemptFailed
        fail.jobId shouldBe msg.jobId
        fail.jobAttemptId shouldBe msg.jobAttemptId
        fail.jobAttemptIndex shouldBe msg.jobAttemptIndex
        fail.jobAttemptRetry shouldBe msg.jobAttemptRetry
        fail.jobAttemptDelayBeforeRetry shouldBe null
        fail.jobAttemptError.error.deserialize()!!::class.java.name shouldBe RetryDelayHasWrongReturnType::class.java.name
    }

    "Should throw when getRetryDelay throw an exception" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        // with
        val msg = getRunJob(TestWithBuggyRetry::class.java.name, input, null)

        // when
        coroutineScope {
            worker.handle(msg)
        }

        // then
        slots.size shouldBe 2

        slots[0] shouldBe getJobAttemptStarted(msg)

        (slots[1] is JobAttemptFailed) shouldBe true
        val fail = slots[1] as JobAttemptFailed
        fail.jobId shouldBe msg.jobId
        fail.jobAttemptId shouldBe msg.jobAttemptId
        fail.jobAttemptIndex shouldBe msg.jobAttemptIndex
        fail.jobAttemptRetry shouldBe msg.jobAttemptRetry
        fail.jobAttemptDelayBeforeRetry shouldBe null
        fail.jobAttemptError.error.deserialize()!!::class.java.name shouldBe IllegalArgumentException::class.java.name
    }

    "Should be able to access context from task" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        // with
        val msg = getRunJob(TestWithContext::class.java.name, input, null)

        // when
        coroutineScope {
            worker.handle(msg)
        }

        // then
        slots.size shouldBe 2

        slots[0] shouldBe getJobAttemptStarted(msg)

        slots[1] shouldBe JobAttemptCompleted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptIndex = msg.jobAttemptIndex,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobOutput = JobOutput(SerializedData.from("72"))
        )
    }

    "Should throw when Worker.getContext() called outside of a processing thread " {
        shouldThrow<JobAttemptContextRetrievedOutsideOfProcessingThread> {
            Worker.context
        }
    }

    "Should throw ProcessingTimeout if processing time is too long" {
        val types = listOf(Int::class.java.name, String::class.java.name)
        val input = listOf(2, "3").map { SerializedData.from(it) }
        // with
        val msg = getRunJob(TestWithTimeout::class.java.name, input, types)

        // when
        coroutineScope {
            worker.handle(msg)
        }

        // then
        slots.size shouldBe 2

        slots[0] shouldBe getJobAttemptStarted(msg)

        (slots[1] is JobAttemptFailed) shouldBe true
        val fail = slots[1] as JobAttemptFailed
        fail.jobId shouldBe msg.jobId
        fail.jobAttemptId shouldBe msg.jobAttemptId
        fail.jobAttemptIndex shouldBe msg.jobAttemptIndex
        fail.jobAttemptRetry shouldBe msg.jobAttemptRetry
        fail.jobAttemptDelayBeforeRetry shouldBe null
        fail.jobAttemptError.error.deserialize()!!::class.java.name shouldBe ProcessingTimeout::class.java.name
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
    @Suppress("unused") fun getRetryDelay(context: JobAttemptContext): Float? = if (context.exception is IllegalStateException) 3F else 0F
}

internal class TestWithBuggyRetry {
    @Suppress("unused") fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()
    @Suppress("unused") fun getRetryDelay(context: JobAttemptContext): Float? = if (context.exception is IllegalStateException) throw IllegalArgumentException() else 3F
}

internal class TestWithBadRetryType {
    @Suppress("unused") fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()
    @Suppress("unused") fun getRetryDelay(context: JobAttemptContext) = 3
}

internal class TestWithConstructor(val value: String) {
    @Suppress("unused") fun handle(i: Int, j: String) = (i * j.toInt()).toString()
}

internal class TestWithContext() {
    @Suppress("unused") fun handle(i: Int, j: String) = (i * j.toInt() * Worker.context.jobAttemptIndex.int).toString()
}

internal class TestWithTimeout() {
    @Suppress("unused") fun handle(i: Int, j: String): String {
        Thread.sleep(400)

        return (i * j.toInt() * Worker.context.jobAttemptIndex.int).toString()
    }
}

private fun getRunJob(name: String, input: List<SerializedData>, types: List<String>?) = RunJob(
    jobId = JobId(),
    jobAttemptId = JobAttemptId(),
    jobAttemptIndex = JobAttemptIndex(12),
    jobAttemptRetry = JobAttemptRetry(7),
    jobName = JobName(name),
    jobInput = JobInput(input),
    jobOptions = JobOptions(runningTimeout = .2F),
    jobMeta = JobMeta().setParameterTypes(types)
)

private fun getJobAttemptStarted(msg: RunJob) = JobAttemptStarted(
    jobId = msg.jobId,
    jobAttemptId = msg.jobAttemptId,
    jobAttemptIndex = msg.jobAttemptIndex,
    jobAttemptRetry = msg.jobAttemptRetry
)
