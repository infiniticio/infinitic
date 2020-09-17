package io.infinitic.workflowManager.common.data.workflows

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.interfaces.IntInterface

data class WorkflowMessageIndex
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override var int: kotlin.Int = 0) : IntInterface
