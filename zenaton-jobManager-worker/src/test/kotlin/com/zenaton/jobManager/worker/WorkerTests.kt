package com.zenaton.jobManager.worker

import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.common.data.JobAttemptId
import com.zenaton.jobManager.common.data.JobAttemptIndex
import com.zenaton.jobManager.common.data.JobAttemptRetry
import com.zenaton.jobManager.common.data.JobId
import com.zenaton.jobManager.common.data.JobInput
import com.zenaton.jobManager.common.data.JobMeta
import com.zenaton.jobManager.common.data.JobName
import com.zenaton.jobManager.common.messages.RunJob
import com.zenaton.jobManager.data.AvroSerializedData
import com.zenaton.jobManager.data.AvroSerializedDataType
import com.zenaton.jobManager.messages.AvroJobAttemptCompleted
import com.zenaton.jobManager.messages.AvroJobAttemptStarted
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.worker.avro.AvroDispatcher
import com.zenaton.jobManager.worker.avro.AvroWorker
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.nio.ByteBuffer

class WorkerTests : StringSpec({
    val avroDispatcher = mockk<AvroDispatcher>()
    val slots = mutableListOf<AvroEnvelopeForJobEngine>()
    every { avroDispatcher.toJobEngine(capture(slots)) } just Runs

    val worker = AvroWorker()
    worker.avroDispatcher = avroDispatcher

    // ensure slots are emptied between each test
    beforeTest {
        slots.clear()
    }

    "Should be able to run a default method with 2 parameters" {
        val input = listOf(2, "3").map { SerializedData.from(it) }
        val types = Test::class.java.methods.filter { it.name == "handle" }[0].parameterTypes.map { it.name }

        // with
        val msg = RunJob(
            jobId = JobId(),
            jobAttemptId = JobAttemptId(),
            jobAttemptIndex = JobAttemptIndex(0),
            jobAttemptRetry = JobAttemptRetry(0),
            jobName = JobName(Test::class.java.name),
            jobInput = JobInput(input),
            jobMeta = JobMeta(mapOf("javaParameterTypes" to SerializedData.from(types)))
        )
        // when
        worker.handle(AvroConverter.addEnvelopeToWorkerMessage(AvroConverter.toAvroMessage(msg)))
        // then
        AvroConverter.removeEnvelopeFromJobEngineMessage(slots[0]) shouldBe AvroJobAttemptStarted.newBuilder()
            .setJobId(msg.jobId.id)
            .setJobAttemptId(msg.jobAttemptId.id)
            .setJobAttemptIndex(msg.jobAttemptIndex.int)
            .setJobAttemptRetry(msg.jobAttemptRetry.int)
            .build()

        AvroConverter.removeEnvelopeFromJobEngineMessage(slots[1]) shouldBe AvroJobAttemptCompleted.newBuilder()
            .setJobId(msg.jobId.id)
            .setJobAttemptId(msg.jobAttemptId.id)
            .setJobAttemptIndex(msg.jobAttemptIndex.int)
            .setJobAttemptRetry(msg.jobAttemptRetry.int)
            .setJobOutput(getAvroSerializedDataFromJson("\"6\""))
            .build()
    }

    "Should be able to run an explicit method with 2 parameters" {
        val input = listOf(3, "3").map { SerializedData.from(it) }
        val types = Test::class.java.methods.filter { it.name == "handle" }[0].parameterTypes.map { it.name }

        // with
        val msg = RunJob(
            jobId = JobId(),
            jobAttemptId = JobAttemptId(),
            jobAttemptIndex = JobAttemptIndex(0),
            jobAttemptRetry = JobAttemptRetry(0),
            jobName = JobName("${Test::class.java.name}::handle"),
            jobInput = JobInput(input),
            jobMeta = JobMeta(mapOf("javaParameterTypes" to SerializedData.from(types)))
        )
        // when
        worker.handle(AvroConverter.addEnvelopeToWorkerMessage(AvroConverter.toAvroMessage(msg)))
        // then
        AvroConverter.removeEnvelopeFromJobEngineMessage(slots[0]) shouldBe AvroJobAttemptStarted.newBuilder()
            .setJobId(msg.jobId.id)
            .setJobAttemptId(msg.jobAttemptId.id)
            .setJobAttemptIndex(msg.jobAttemptIndex.int)
            .setJobAttemptRetry(msg.jobAttemptRetry.int)
            .build()

        AvroConverter.removeEnvelopeFromJobEngineMessage(slots[1]) shouldBe AvroJobAttemptCompleted.newBuilder()
            .setJobId(msg.jobId.id)
            .setJobAttemptId(msg.jobAttemptId.id)
            .setJobAttemptIndex(msg.jobAttemptIndex.int)
            .setJobAttemptRetry(msg.jobAttemptRetry.int)
            .setJobOutput(getAvroSerializedDataFromJson("\"9\""))
            .build()
    }
})

private class Test {
    fun handle(i: Int, j: String) = (i * j.toInt()).toString()
}

private fun getAvroSerializedDataFromJson(str: String) = AvroSerializedData.newBuilder()
    .setBytes(ByteBuffer.wrap(str.toByteArray()))
    .setType(AvroSerializedDataType.JSON)
    .setMeta(mapOf())
    .build()
