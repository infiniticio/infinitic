package io.infinitic.common.workflows.data.properties

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.tasks.data.bases.Hash

data class PropertyHash
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val hash: String) : Hash(hash)
