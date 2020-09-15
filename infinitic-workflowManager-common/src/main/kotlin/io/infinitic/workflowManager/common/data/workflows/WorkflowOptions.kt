package io.infinitic.workflowManager.common.data.workflows

data class WorkflowOptions(
    val workflowChangeCheckMode: WorkflowChangeCheckMode = WorkflowChangeCheckMode.ALL
)

enum class WorkflowChangeCheckMode {
    NONE, SIMPLE_NAME_ONLY, ALL
}
