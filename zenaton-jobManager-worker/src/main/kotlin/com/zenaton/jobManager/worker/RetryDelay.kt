package com.zenaton.jobManager.worker

internal sealed class RetryDelay

internal data class RetryDelayRetrieved(val value: Float?) : RetryDelay()

internal data class RetryDelayFailed(val e: Throwable?) : RetryDelay()
