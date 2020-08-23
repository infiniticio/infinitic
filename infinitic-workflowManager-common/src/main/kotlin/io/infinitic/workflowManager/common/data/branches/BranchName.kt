package io.infinitic.workflowManager.common.data.branches

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.taskManager.common.data.Name

data class BranchName
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val name: String) : Name(name)
