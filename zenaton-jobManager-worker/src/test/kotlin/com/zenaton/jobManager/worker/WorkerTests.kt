package com.zenaton.jobManager.worker

import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.common.data.JobAttemptContext
import com.zenaton.jobManager.common.data.JobAttemptId
import com.zenaton.jobManager.common.data.JobAttemptIndex
import com.zenaton.jobManager.common.data.JobAttemptRetry
import com.zenaton.jobManager.common.data.JobId
import com.zenaton.jobManager.common.data.JobInput
import com.zenaton.jobManager.common.data.JobMeta
import com.zenaton.jobManager.common.data.JobName
import com.zenaton.jobManager.common.data.JobOptions
import com.zenaton.jobManager.common.data.JobOutput
import com.zenaton.jobManager.common.exceptions.ClassNotFoundDuringJobInstantiation
import com.zenaton.jobManager.common.exceptions.ErrorDuringJobInstantiation
import com.zenaton.jobManager.common.exceptions.InvalidUseOfDividerInJobName
import com.zenaton.jobManager.common.exceptions.MultipleUseOfDividerInJobName
import com.zenaton.jobManager.common.exceptions.NoMethodFoundWithParameterTypes
import com.zenaton.jobManager.common.messages.ForJobEngineMessage
import com.zenaton.jobManager.common.messages.JobAttemptCompleted
import com.zenaton.jobManager.common.messages.JobAttemptFailed
import com.zenaton.jobManager.common.messages.JobAttemptStarted
import com.zenaton.jobManager.common.messages.RunJob
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
    val slots = mutableListOf<ForJobEngineMessage>()
    every { dispatcher.toJobEngine(capture(slots)) } just Runs
    var worker = Worker()
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
            worker.suspendingHandle(msg)
        }
        // then
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
        val msg = getRunJob(TestWithoutRetry::class.java.name, input, types)
        // when
        coroutineScope {
            worker.suspendingHandle(msg)
        }

        // then
        slots[0] shouldBe getJobAttemptStarted(msg)

        slots[1] shouldBe JobAttemptCompleted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptIndex = msg.jobAttemptIndex,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobOutput = JobOutput(SerializedData.from("9"))
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
            worker.suspendingHandle(msg)
        }
        // then
        slots[0] shouldBe getJobAttemptStarted(msg)

        slots[1] shouldBe JobAttemptCompleted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptIndex = msg.jobAttemptIndex,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobOutput = JobOutput(SerializedData.from("6"))
        )
    }

    "Should fail when trying to process an invalid task name " {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunJob("blabla::m1::m2", input, types)

        // when
        coroutineScope {
            worker.suspendingHandle(msg)
        }

        // then
        slots[0] shouldBe getJobAttemptStarted(msg)

        (slots[1] is JobAttemptFailed) shouldBe true
        val fail = slots[1] as JobAttemptFailed
        fail.jobId shouldBe msg.jobId
        fail.jobAttemptId shouldBe msg.jobAttemptId
        fail.jobAttemptIndex shouldBe msg.jobAttemptIndex
        fail.jobAttemptRetry shouldBe msg.jobAttemptRetry
        (fail.jobAttemptError.error.deserialize() is MultipleUseOfDividerInJobName) shouldBe true
    }

    "Should fail when trying to process an unknown task" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunJob("blabla", input, types)

        // when
        Worker.unregister("blabla")

        coroutineScope {
            worker.suspendingHandle(msg)
        }

        // then
        slots[0] shouldBe getJobAttemptStarted(msg)

        (slots[1] is JobAttemptFailed) shouldBe true
        val fail = slots[1] as JobAttemptFailed
        fail.jobId shouldBe msg.jobId
        fail.jobAttemptId shouldBe msg.jobAttemptId
        fail.jobAttemptIndex shouldBe msg.jobAttemptIndex
        fail.jobAttemptRetry shouldBe msg.jobAttemptRetry
        (fail.jobAttemptError.error.deserialize() is ClassNotFoundDuringJobInstantiation) shouldBe true
    }

    "Should fail when if impossible to create new instance" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunJob(TestWithConstructor::class.java.name, input, types)

        // when
        coroutineScope {
            worker.suspendingHandle(msg)
        }

        // then
        slots[0] shouldBe getJobAttemptStarted(msg)

        (slots[1] is JobAttemptFailed) shouldBe true
        val fail = slots[1] as JobAttemptFailed
        fail.jobId shouldBe msg.jobId
        fail.jobAttemptId shouldBe msg.jobAttemptId
        fail.jobAttemptIndex shouldBe msg.jobAttemptIndex
        fail.jobAttemptRetry shouldBe msg.jobAttemptRetry
        (fail.jobAttemptError.error.deserialize() is ErrorDuringJobInstantiation) shouldBe true
    }

    "Should fail when trying to process an unknown method" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        val types = listOf(Int::class.java.name, String::class.java.name)
        // with
        val msg = getRunJob("${TestWithoutRetry::class.java.name}::unknown", input, types)

        // when
        coroutineScope {
            worker.suspendingHandle(msg)
        }

        // then
        slots[0] shouldBe getJobAttemptStarted(msg)

        (slots[1] is JobAttemptFailed) shouldBe true
        val fail = slots[1] as JobAttemptFailed
        fail.jobId shouldBe msg.jobId
        fail.jobAttemptId shouldBe msg.jobAttemptId
        fail.jobAttemptIndex shouldBe msg.jobAttemptIndex
        fail.jobAttemptRetry shouldBe msg.jobAttemptRetry
        (fail.jobAttemptError.error.deserialize() is NoMethodFoundWithParameterTypes) shouldBe true
    }
})

internal class TestWithoutRetry {
    @Suppress("unused")
    fun handle(i: Int, j: String) = (i * j.toInt()).toString()
}

internal class TestWithRetry {
    @Suppress("unused")
    fun handle(i: Int, j: String) = (i * j.toInt()).toString()
    fun delayBeforeRetry(context: JobAttemptContext) = 3F
}

internal class TestWithConstructor(val value: String) {
    fun handle(i: Int, j: String) = (i * j.toInt()).toString()
}

private fun getRunJob(name: String, input: List<SerializedData>, types: List<String>) = RunJob(
    jobId = JobId(),
    jobAttemptId = JobAttemptId(),
    jobAttemptIndex = JobAttemptIndex(12),
    jobAttemptRetry = JobAttemptRetry(7),
    jobName = JobName(name),
    jobInput = JobInput(input),
    jobOptions = JobOptions(runningTimeout = 2F),
    jobMeta = JobMeta().setParameterTypes(types)
)

private fun getJobAttemptStarted(msg: RunJob) = JobAttemptStarted(
    jobId = msg.jobId,
    jobAttemptId = msg.jobAttemptId,
    jobAttemptIndex = msg.jobAttemptIndex,
    jobAttemptRetry = msg.jobAttemptRetry
)
