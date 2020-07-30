package com.zenaton.jobManager.client

import com.zenaton.common.data.interfaces.IdInterface
import com.zenaton.jobManager.client.avro.AvroDispatcher
import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.common.data.JobId
import com.zenaton.jobManager.data.AvroSerializedData
import com.zenaton.jobManager.data.AvroSerializedDataType
import com.zenaton.jobManager.messages.AvroDispatchJob
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import java.nio.ByteBuffer

class ClientTests : StringSpec({
    val avroDispatcher = mockk<AvroDispatcher>()
    var avro = slot<AvroEnvelopeForJobEngine>()
    every { avroDispatcher.toJobEngine(capture(avro)) } just Runs
    val client = Client()
    client.dispatcher = Dispatcher(avroDispatcher)

    "Should be able to dispatch method without parameter" {
        // when
        val job = client.dispatch<FakeTask> { m1() }
        // then
        avro.isCaptured shouldBe true
        AvroConverter.removeEnvelopeFromJobEngineMessage(avro.captured) shouldBe AvroDispatchJob.newBuilder()
            .setJobId(job.jobId.id)
            .setJobInput(listOf())
            .setJobName("${FakeTask::class.java.name}::m1")
            .setJobMeta(mapOf("javaParameterTypes" to getAvroSerializedDataFromJson("[]")))
            .build()
    }

    "Should be able to dispatch a method with a primitive as parameter" {
        // when
        val job = client.dispatch<FakeTask> { m1(0) }
        // then
        avro.isCaptured shouldBe true
        AvroConverter.removeEnvelopeFromJobEngineMessage(avro.captured) shouldBe AvroDispatchJob.newBuilder()
            .setJobId(job.jobId.id)
            .setJobInput(listOf(getAvroSerializedDataFromJson("0")))
            .setJobName("${FakeTask::class.java.name}::m1")
            .setJobMeta(mapOf("javaParameterTypes" to getAvroSerializedDataFromJson("[\"int\"]")))
            .build()
    }

    "Should be able to dispatch a method with multiple definition" {
        // when
        val job = client.dispatch<FakeTask> { m1("a") }
        // then
        avro.isCaptured shouldBe true
        AvroConverter.removeEnvelopeFromJobEngineMessage(avro.captured) shouldBe AvroDispatchJob.newBuilder()
            .setJobId(job.jobId.id)
            .setJobInput(listOf(getAvroSerializedDataFromJson("\"a\"")))
            .setJobName("${FakeTask::class.java.name}::m1")
            .setJobMeta(mapOf("javaParameterTypes" to getAvroSerializedDataFromJson("[\"java.lang.String\"]")))
            .build()
    }

    "Should be able to dispatch a method with multiple parameters" {
        // when
        val job = client.dispatch<FakeTask> { m2(0, "a") }
        // then
        avro.isCaptured shouldBe true
        AvroConverter.removeEnvelopeFromJobEngineMessage(avro.captured) shouldBe AvroDispatchJob.newBuilder()
            .setJobId(job.jobId.id)
            .setJobInput(listOf(getAvroSerializedDataFromJson("0"), getAvroSerializedDataFromJson("\"a\"")))
            .setJobName("${FakeTask::class.java.name}::m2")
            .setJobMeta(mapOf("javaParameterTypes" to getAvroSerializedDataFromJson("[\"int\",\"java.lang.String\"]")))
            .build()
    }

    "Should be able to dispatch a method with an interface as parameter" {
        // when
        val jobId = JobId()
        val job = client.dispatch<FakeTask> { m2(jobId) }
        // then
        avro.isCaptured shouldBe true
        AvroConverter.removeEnvelopeFromJobEngineMessage(avro.captured) shouldBe AvroDispatchJob.newBuilder()
            .setJobId(job.jobId.id)
            .setJobInput(listOf(getAvroSerializedDataFromJson("\"${jobId.id}\"")))
            .setJobName("${FakeTask::class.java.name}::m2")
            .setJobMeta(mapOf("javaParameterTypes" to getAvroSerializedDataFromJson("[\"${IdInterface::class.java.name}\"]")))
            .build()
    }
})

private fun getAvroSerializedDataFromJson(str: String) = AvroSerializedData.newBuilder()
    .setBytes(ByteBuffer.wrap(str.toByteArray()))
    .setType(AvroSerializedDataType.JSON)
    .setMeta(mapOf())
    .build()

private fun getAvroSerializedDataFromNull() = AvroSerializedData.newBuilder()
    .setBytes(ByteBuffer.wrap("".toByteArray()))
    .setType(AvroSerializedDataType.NULL)
    .setMeta(mapOf())
    .build()

private interface FakeTask {
    fun m1()
    fun m1(i: Int): String
    fun m1(str: String): Any?
    fun m2(p1: Int, p2: String): String
    fun m2(id: IdInterface): String
}
