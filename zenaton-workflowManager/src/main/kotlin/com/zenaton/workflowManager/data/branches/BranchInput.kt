package com.zenaton.workflowManager.data.branches

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.commons.data.SerializedParameter

data class BranchInput
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue val input: List<SerializedParameter>)
