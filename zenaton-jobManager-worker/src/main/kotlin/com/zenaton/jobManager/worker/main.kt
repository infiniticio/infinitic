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
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.worker.avro.AvroDispatcher
import com.zenaton.jobManager.worker.avro.AvroWorker

fun main() {
    val input = listOf(2, "3", JobName("test")).map { SerializedData.from(it) }
    val ms = Test::class.java.methods.filter { it.name == "handle" }
    val types = ms[0].parameterTypes.map { it.name }

    val msg = RunJob(
        jobId = JobId(),
        jobAttemptId = JobAttemptId(),
        jobAttemptIndex = JobAttemptIndex(0),
        jobAttemptRetry = JobAttemptRetry(0),
        jobName = JobName(Test::class.java.name),
        jobInput = JobInput(input),
        jobMeta = JobMeta(mapOf(Worker.META_PARAMETER_TYPES to SerializedData.from(types)))
    )
    val avro = AvroConverter.addEnvelopeToWorkerMessage(AvroConverter.toAvroMessage(msg))

    val worker = AvroWorker()
    worker.avroDispatcher = FakeAvroDispatcher()
    worker.handle(avro)
    val m = Test::handle
}

class Test {
    fun handle(i: Int, j: String, k: JobName) = (i * j.toInt()).toString() + k.name
}

class FakeAvroDispatcher : AvroDispatcher {
    override fun toJobEngine(msg: AvroEnvelopeForJobEngine) {
        println("FakeAvroDispatcher: ${msg.type} ${AvroConverter.removeEnvelopeFromJobEngineMessage(msg)}")
    }
}
