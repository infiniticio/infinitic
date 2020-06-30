package com.zenaton.jobManager.avroEngines.sync

import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.avroConverter.AvroConverter
import com.zenaton.jobManager.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.messages.AvroJobAttemptCompleted
import com.zenaton.jobManager.messages.AvroJobAttemptFailed
import com.zenaton.jobManager.messages.AvroJobAttemptStarted
import com.zenaton.jobManager.messages.AvroRunJob
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker

internal interface SyncWorker {
    var avroDispatcher: AvroDispatcher

    enum class Status {
        SUCCESS,
        FAIL_WITH_RETRY,
        FAIL_WITHOUT_RETRY
    }

    fun handle(msg: AvroEnvelopeForWorker)

    fun sendJobStarted(avro: AvroRunJob) {
        // send start
        val avroJobAttemptStarted = AvroJobAttemptStarted.newBuilder()
            .setJobId(avro.jobId)
            .setJobAttemptId(avro.jobAttemptId)
            .setJobAttemptRetry(avro.jobAttemptRetry)
            .setJobAttemptIndex(avro.jobAttemptIndex)
            .build()
        avroDispatcher.toJobEngine(AvroConverter.addEnvelopeToJobEngineMessage(avroJobAttemptStarted))
    }

    fun sendJobFailed(avro: AvroRunJob, error: Exception? = null, delay: Float? = null) {
        // send failure
        val avroJobAttemptFailed = AvroJobAttemptFailed.newBuilder()
            .setJobId(avro.jobId)
            .setJobAttemptId(avro.jobAttemptId)
            .setJobAttemptRetry(avro.jobAttemptRetry)
            .setJobAttemptIndex(avro.jobAttemptIndex)
            .setJobAttemptError(AvroConverter.toAvroSerializedData(SerializedData.from(error)))
            .setJobAttemptDelayBeforeRetry(delay)
            .build()
        avroDispatcher.toJobEngine(AvroConverter.addEnvelopeToJobEngineMessage(avroJobAttemptFailed))
    }

    fun sendJobCompleted(avro: AvroRunJob, out: Any? = null) {
        // send completion
        val avroJobAttemptCompleted = AvroJobAttemptCompleted.newBuilder()
            .setJobId(avro.jobId)
            .setJobAttemptId(avro.jobAttemptId)
            .setJobAttemptRetry(avro.jobAttemptRetry)
            .setJobAttemptIndex(avro.jobAttemptIndex)
            .setJobOutput(AvroConverter.toAvroSerializedData(SerializedData.from(out)))
            .build()
        avroDispatcher.toJobEngine(AvroConverter.addEnvelopeToJobEngineMessage(avroJobAttemptCompleted))
    }
}
