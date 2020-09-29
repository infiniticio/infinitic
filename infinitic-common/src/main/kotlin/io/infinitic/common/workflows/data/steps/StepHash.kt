package io.infinitic.common.workflows.data.steps

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.tasks.data.bases.Hash

data class StepHash
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val hash: String) : Hash(hash)
