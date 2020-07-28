package com.zenaton.workflowManager.data.branches

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.common.data.interfaces.OutputInterface

data class BranchOutput
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val output: SerializedData) : OutputInterface
