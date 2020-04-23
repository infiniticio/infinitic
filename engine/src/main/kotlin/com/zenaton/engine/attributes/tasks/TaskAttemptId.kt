package com.zenaton.engine.attributes.tasks

import com.zenaton.engine.attributes.types.Id
import java.util.UUID

data class TaskAttemptId(override val id: String = UUID.randomUUID().toString()) : Id(id)
