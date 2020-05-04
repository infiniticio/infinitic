package com.zenaton

import java.time.Instant

data class Workflow(
    val id: String,
    val name: String,
//    val inputs: List<AnyValue>,
    val status: String,
    val tags: List<String>,
    val startedAt: Instant,
    val completedAt: Instant
//    val traces: List<WorkflowTrace>
) {
}

data class Task(val id: String, val name: String, val status: String, val dispatchedAt: Instant) {
    var startedAt: Instant? = null
    var completedAt: Instant? = null
    var failedAt: Instant? = null
}
