package com.zenaton.engine.taskAttempts.data

import com.zenaton.engine.interfaces.data.IdInterface
import java.util.UUID

data class TaskAttemptId(override val id: String = UUID.randomUUID().toString()) : IdInterface
