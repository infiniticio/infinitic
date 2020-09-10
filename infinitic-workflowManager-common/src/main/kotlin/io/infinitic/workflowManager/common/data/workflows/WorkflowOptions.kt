package io.infinitic.workflowManager.common.data.workflows

data class WorkflowOptions(
    val workflowIntegrityCheckMode: WorkflowIntegrityCheckMode = WorkflowIntegrityCheckMode.ALL
)

enum class WorkflowIntegrityCheckMode {
    NONE, SIMPLE_NAME_ONLY, ALL
}
