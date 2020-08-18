package com.zenaton.jobManager.common.data

data class JobAttemptContext(
    val jobId: JobId,
    val jobAttemptId: JobAttemptId,
    val jobAttemptIndex: JobAttemptIndex,
    val jobAttemptRetry: JobAttemptRetry,
    var exception: Throwable? = null,
    val jobMeta: JobMeta,
    val jobOptions: JobOptions
)
