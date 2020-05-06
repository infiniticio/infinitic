package com.zenaton.api.task.models

import java.time.Instant

data class Task(val id: String, val name: String, val status: String, val dispatchedAt: Instant) {
    var startedAt: Instant? = null
    var completedAt: Instant? = null
    var failedAt: Instant? = null
}
