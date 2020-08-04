package com.zenaton.jobManager.worker

import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.common.data.JobAttemptId
import com.zenaton.jobManager.common.data.JobAttemptIndex
import com.zenaton.jobManager.common.data.JobAttemptRetry
import com.zenaton.jobManager.common.data.JobId
import com.zenaton.jobManager.common.data.JobInput
import com.zenaton.jobManager.common.data.JobMeta
import com.zenaton.jobManager.common.data.JobName
import com.zenaton.jobManager.common.data.JobOutput
import com.zenaton.jobManager.common.messages.ForJobEngineMessage
import com.zenaton.jobManager.common.messages.JobAttemptCompleted
import com.zenaton.jobManager.common.messages.JobAttemptStarted
import com.zenaton.jobManager.common.messages.RunJob
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk

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
        val msg = getRunJob(input, types)
        // when
        worker.handle(msg)
        // then
        slots[0] shouldBe JobAttemptStarted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptIndex = msg.jobAttemptIndex,
            jobAttemptRetry = msg.jobAttemptRetry
        )
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
        val msg = getRunJob(input, types)
        // when
        worker.handle(msg)
        // then
        slots[0] shouldBe JobAttemptStarted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptIndex = msg.jobAttemptIndex,
            jobAttemptRetry = msg.jobAttemptRetry
        )
        slots[1] shouldBe JobAttemptCompleted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptIndex = msg.jobAttemptIndex,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobOutput = JobOutput(SerializedData.from("9"))
        )
    }
})

private class Test {
    @Suppress("unused")
    fun handle(i: Int, j: String) = (i * j.toInt()).toString()
}

private fun getRunJob(input: List<SerializedData>, types: List<String>) = RunJob(
    jobId = JobId(),
    jobAttemptId = JobAttemptId(),
    jobAttemptIndex = JobAttemptIndex(12),
    jobAttemptRetry = JobAttemptRetry(7),
    jobName = JobName(Test::class.java.name),
    jobInput = JobInput(input),
    jobMeta = JobMeta.forParameterTypes(types)
)
