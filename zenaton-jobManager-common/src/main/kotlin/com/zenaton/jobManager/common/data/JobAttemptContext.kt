package com.zenaton.jobManager.common.data

data class JobAttemptContext(
    val jobId: JobId,
    val jobAttemptId: JobAttemptId,
    val jobAttemptIndex: JobAttemptIndex,
    val jobAttemptRetry: JobAttemptRetry,
    val exception: Exception? = null,
    val meta: JobMeta,
    val options: JobOptions
)
