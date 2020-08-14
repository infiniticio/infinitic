package com.zenaton.jobManager.worker

internal sealed class RetryDelayCommand

internal data class RetryDelayRetrieved(val value: Float?) : RetryDelayCommand()

internal data class RetryDelayFailed(val e: Throwable?) : RetryDelayCommand()
