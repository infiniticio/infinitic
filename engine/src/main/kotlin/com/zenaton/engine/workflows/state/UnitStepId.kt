package com.zenaton.engine.workflows.state

import java.util.UUID

data class UnitStepId(val uuid: String = UUID.randomUUID().toString())
