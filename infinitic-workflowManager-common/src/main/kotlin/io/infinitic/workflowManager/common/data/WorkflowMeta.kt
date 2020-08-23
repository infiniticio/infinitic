package io.infinitic.workflowManager.common.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.data.Meta

data class WorkflowMeta
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val meta: MutableMap<String, SerializedData> = mutableMapOf()) : Meta(meta)
