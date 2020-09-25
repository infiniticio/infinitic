package io.infinitic.common.workflowManager.data.methodRuns

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.taskManager.data.bases.Id
import java.util.UUID

data class MethodRunId
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val id: String = UUID.randomUUID().toString()) : Id(id)