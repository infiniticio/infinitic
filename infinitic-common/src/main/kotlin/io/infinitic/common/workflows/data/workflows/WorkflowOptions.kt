package io.infinitic.common.workflows.data.workflows

data class WorkflowOptions(
    val workflowChangeCheckMode: WorkflowChangeCheckMode = WorkflowChangeCheckMode.ALL
)

enum class WorkflowChangeCheckMode {
    NONE, SIMPLE_NAME_ONLY, ALL
}
