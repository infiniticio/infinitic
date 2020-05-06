package com.zenaton.api.workflow.models

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