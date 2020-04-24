package com.zenaton.engine.data.tasks

import com.zenaton.engine.data.types.Id
import java.util.UUID

data class TaskAttemptId(override val id: String = UUID.randomUUID().toString()) : Id(id)
