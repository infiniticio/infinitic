package io.infinitic.workflowManager.common.data.properties

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.taskManager.common.data.bases.Hash

data class PropertyHash
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val hash: String) : Hash(hash)
