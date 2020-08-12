package com.zenaton.jobManager.worker

sealed class RetryDelayCommand

data class RetryDelayRetrieved(val value: Float?) : RetryDelayCommand()

data class RetryDelayRetrievalException(val e: Throwable) : RetryDelayCommand()
