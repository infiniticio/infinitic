package com.zenaton.engine.workflows.state

import com.zenaton.engine.common.attributes.Id
import java.util.UUID

data class ActionId(override val id: String = UUID.randomUUID().toString()) : Id(id)
